package com.axonivy.utils.estimator.internal;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.internal.model.CommonElement;
import com.axonivy.utils.estimator.internal.model.ProcessElement;
import com.axonivy.utils.estimator.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public class WorkflowTime {
	private ProcessGraph processGraph;
	private Map<String, Duration> durationOverrides;

	public WorkflowTime(Map<String, Duration> durationOverrides) {
		this.processGraph = new ProcessGraph();		
		this.durationOverrides = durationOverrides;
	}

	public Duration getDuration(TaskAndCaseModifier task, TaskConfig taskConfig, UseCase useCase) {
		String key = processGraph.getTaskId(task, taskConfig);		
		return this.durationOverrides.getOrDefault(key, getDurationByTaskScript(taskConfig, useCase));	
	}
	
	protected Map<SequenceFlow, List<ProcessElement>> getInternalPath(Map<SequenceFlow, List<ProcessElement>> internalPath, boolean withTaskEnd){		
		Map<SequenceFlow, List<ProcessElement>> path = new LinkedHashMap<>();
				
		//Priority the path go to end first
		for(SequenceFlow sf : internalPath.keySet()) {
			ProcessElement last = getLast(internalPath.get(sf));
			if(withTaskEnd && last.getElement() instanceof TaskEnd) {
				path.put(sf, internalPath.get(sf));
			} else if (!withTaskEnd && last.getElement() instanceof TaskEnd == false){
				path.put(sf, internalPath.get(sf));
			}
		}
		
		return path;
	}
	
	protected Duration calculateTotalDuration(List<ProcessElement> path, UseCase useCase) {
		return calculateTotalDuration(path, useCase, durationOverrides);
	}
	
	private Duration calculateTotalDuration(List<ProcessElement> path, UseCase useCase, Map<String, Duration> durationOverrides) {

		// convert to both Estimated Task and alternative
		List<Duration> totalWithEnd = new ArrayList<>();		
		Duration total = Duration.ZERO;
				
		for(int i = 0; i < path.size(); i++) {
			ProcessElement element = path.get(i);
		
			// CommonElement(RequestStart)
			if (element.getElement() instanceof RequestStart) {
				continue;
			}
			
			if (element.getElement()instanceof TaskAndCaseModifier && processGraph.isSystemTask((TaskAndCaseModifier) element.getElement())) {
				continue;
			}
			
			if (element instanceof TaskParallelGroup) {
				Duration durationWithEndTask = getMaxTotalFromTaskParallelGroupWithTaskEnd((TaskParallelGroup) element, useCase, durationOverrides);
				totalWithEnd.add(durationWithEndTask);
				
				Duration maxDuration = getMaxTotalFromTaskParallelGroupWithoutTaskEnd((TaskParallelGroup) element, useCase, durationOverrides);
				total = total.plus(maxDuration);
				continue;
			}
			
			// CommonElement(SingleTaskCreator)
			if (element.getElement() instanceof SingleTaskCreator) {				
				SingleTaskCreator singleTask = (SingleTaskCreator)element.getElement();
				Duration taskDuration = getDuration(singleTask, singleTask.getTaskConfig(), useCase);
				total = total.plus(taskDuration);
				continue;
			}
			
			if (element instanceof CommonElement && element.getElement() instanceof SequenceFlow) {
				SequenceFlow sequenceFlow = (SequenceFlow) element.getElement();
				if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
					TaskConfig startTask = processGraph.getStartTaskConfig(sequenceFlow);					
					Duration startTaskDuration = getDuration((TaskAndCaseModifier)sequenceFlow.getSource(), startTask, useCase);
					total = total.plus(startTaskDuration);
					continue;
				}
			}
		}
		
		Duration maxTotal = Stream.concat(totalWithEnd.stream(), Stream.of(total)).max(Comparator.naturalOrder()).orElse(Duration.ZERO);
		return maxTotal;		
	}
	
	private Duration getMaxTotalFromTaskParallelGroupWithTaskEnd(TaskParallelGroup group, UseCase useCase, Map<String, Duration> durationOverrides) {
		return getMaxTotalFromTaskParallelGroup((TaskParallelGroup) group, useCase, durationOverrides, true);
	}

	private Duration getMaxTotalFromTaskParallelGroupWithoutTaskEnd(TaskParallelGroup group, UseCase useCase, Map<String, Duration> durationOverrides) {
		return getMaxTotalFromTaskParallelGroup((TaskParallelGroup) group, useCase, durationOverrides, false);
	}
	
	private Duration getMaxTotalFromTaskParallelGroup(TaskParallelGroup group, UseCase useCase, Map<String, Duration> durationOverrides, boolean withTaskEnd) {
		Map<SequenceFlow, List<ProcessElement>> internalPath = getInternalPath(group.getInternalPaths(), withTaskEnd);
		Map<SequenceFlow, Duration> result = new HashMap<>();
		
		for (Entry<SequenceFlow, List<ProcessElement>> entry : internalPath.entrySet()) {
			 Duration total = calculateTotalDuration(entry.getValue(), useCase, durationOverrides);
			 result.put(entry.getKey(), total);
		}
		
		Duration maxTotal = result.values().stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
		
		return maxTotal;
	}
	
	private Duration getDurationByTaskScript(TaskConfig task, UseCase useCase) {
		List<String> prefixs = new ArrayList<String>(Arrays.asList("WfEstimate.setEstimate"));
		if(useCase != null) {
			prefixs.add("UseCase." + useCase.name());
		}

		String wfEstimateCode = processGraph.getCodeLineByPrefix(task, prefixs.toArray(new String[0]));
		if (isNotEmpty(wfEstimateCode)) {
			String result = StringUtils.substringBetween(wfEstimateCode, "(", "UseCase");
			int amount = Integer.parseInt(result.substring(0, result.indexOf(",")));
			String unit = result.substring(result.indexOf(".") + 1, result.lastIndexOf(","));

			switch (TimeUnit.valueOf(unit.toUpperCase())) {
				case DAYS:
	                return Duration.ofDays(amount);
	            case HOURS:
	                return Duration.ofHours(amount);
	            case MINUTES:
	                return Duration.ofMinutes(amount);
	            case SECONDS:
	                return Duration.ofSeconds(amount);
	            default:
	                // Handle any unexpected TimeUnit
	                break;
			}		
		}

		return Duration.ofHours(0);
	}
	
	private <T> T getLast(List<T> elements) {
		return elements.stream().reduce((first, second) -> second).orElse(null);
	}
}

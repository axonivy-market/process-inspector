package com.axonivy.utils.process.analyzer.internal;

import static org.apache.commons.lang3.StringUtils.isBlank;
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

import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
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

	public Duration getDuration(TaskAndCaseModifier task, TaskConfig taskConfig, Enum<?> useCase) {
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
	
	protected Duration calculateTotalDuration(Map<ProcessElement, List<ProcessElement>> path, Enum<?> useCase) {
		return calculateTotalDuration(path, useCase, durationOverrides);
	}
	
	private Duration calculateTotalDuration(Map<ProcessElement, List<ProcessElement>> paths, Enum<?> useCase, Map<String, Duration> durationOverrides) {

		// convert to both detected task and alternative		
		List<Duration> totalOfPaths = new ArrayList<>();
		for (Entry<ProcessElement, List<ProcessElement>> path : paths.entrySet()) {
			List<Duration> totalWithEnd = new ArrayList<>();		
			Duration total = Duration.ZERO;
			for (ProcessElement element : path.getValue()) {

				// CommonElement(RequestStart)
				if (processGraph.isRequestStart(element.getElement())) {
					continue;
				}

				if (processGraph.isTaskAndCaseModifier(element.getElement())
						&& processGraph.isSystemTask(element.getElement())) {
					continue;
				}

				if (element instanceof TaskParallelGroup) {
					TaskParallelGroup taskGroup = (TaskParallelGroup) element;
					Duration durationWithEndTask = getMaxTotalFromTaskParallelGroupWithTaskEnd(taskGroup, useCase, durationOverrides);
					totalWithEnd.add(durationWithEndTask);

					Duration maxDuration = getMaxTotalFromTaskParallelGroupWithoutTaskEnd(taskGroup, useCase, durationOverrides);
					total = total.plus(maxDuration);
					continue;
				}

				// CommonElement(SingleTaskCreator)
				if (processGraph.isSingleTaskCreator(element.getElement())) {
					SingleTaskCreator singleTask = (SingleTaskCreator) element.getElement();
					Duration taskDuration = getDuration(singleTask, singleTask.getTaskConfig(), useCase);
					total = total.plus(taskDuration);
					continue;
				}

				if (element instanceof CommonElement && processGraph.isSequenceFlow(element.getElement())) {
					SequenceFlow sequenceFlow = (SequenceFlow) element.getElement();
					if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
						TaskConfig startTask = processGraph.getStartTaskConfig(sequenceFlow);
						Duration startTaskDuration = getDuration((TaskAndCaseModifier) sequenceFlow.getSource(), startTask, useCase);
						total = total.plus(startTaskDuration);
						continue;
					}
				}
			}
			Duration maxTotal = Stream.concat(totalWithEnd.stream(), Stream.of(total)).max(Comparator.naturalOrder()).orElse(Duration.ZERO);
			totalOfPaths.add(maxTotal);
		}
		
		return totalOfPaths.stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);		
	}
	
	private Duration getMaxTotalFromTaskParallelGroupWithTaskEnd(TaskParallelGroup group, Enum<?> useCase, Map<String, Duration> durationOverrides) {
		return getMaxTotalFromTaskParallelGroup((TaskParallelGroup) group, useCase, durationOverrides, true);
	}

	private Duration getMaxTotalFromTaskParallelGroupWithoutTaskEnd(TaskParallelGroup group, Enum<?> useCase, Map<String, Duration> durationOverrides) {
		return getMaxTotalFromTaskParallelGroup((TaskParallelGroup) group, useCase, durationOverrides, false);
	}
	
	private Duration getMaxTotalFromTaskParallelGroup(TaskParallelGroup group, Enum<?> useCase, Map<String, Duration> durationOverrides, boolean withTaskEnd) {
		Map<SequenceFlow, List<ProcessElement>> internalPath = getInternalPath(group.getInternalPaths(), withTaskEnd);
		Map<SequenceFlow, Duration> result = new HashMap<>();
		
		for (Entry<SequenceFlow, List<ProcessElement>> entry : internalPath.entrySet()) {
			Duration total = Duration.ZERO;
			if (group.getElement() != null) {
				TaskConfig startTask = processGraph.getStartTaskConfig(entry.getKey());
				Duration startTaskDuration = getDuration((TaskAndCaseModifier) group.getElement(), startTask, useCase);
				total.plus(startTaskDuration);
			}
			
			Map<ProcessElement, List<ProcessElement>> path = Map.of(new CommonElement(entry.getKey()), entry.getValue());
			
			Duration totalFromSubPath = calculateTotalDuration(path, useCase, durationOverrides);

			result.put(entry.getKey(), total.plus(totalFromSubPath));
		}
		
		Duration maxTotal = result.values().stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
		
		return maxTotal;
	}
	
	private Duration getDurationByTaskScript(TaskConfig task, Enum<?> useCase) {
		String useCasePrefix = getUseCasePrefix(useCase);
		List<String> prefixs = Arrays.asList("APAConfig.setEstimate", useCasePrefix);		
		
		String wfEstimateCode = processGraph.getCodeLineByPrefix(task, prefixs.toArray(new String[0]));
		if (isNotEmpty(wfEstimateCode)) {
			String[] result = StringUtils.substringsBetween(wfEstimateCode,  "(",  ")")[0].split(",");
			int amount = Integer.parseInt(result[0]);
			TimeUnit unit = getTimeUnit(result[1]);

			switch (unit) {
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

	private TimeUnit getTimeUnit(String timeUnit) {
		if(isBlank(timeUnit)) {
			return null;
		}
		
		if(timeUnit.startsWith("TimeUnit.")) {
			//TimeUnit.HOURS
			return TimeUnit.valueOf(timeUnit.split("\\.")[1]); 
		} else {
			//HOURS
			return TimeUnit.valueOf(timeUnit);
		}
	}
	private String getUseCasePrefix(Enum<?> useCase) {
		String usePrefix = StringUtils.EMPTY;
		if (useCase != null) {
			usePrefix = "." + useCase.name();
		}

		return usePrefix;
	}
	
	private <T> T getLast(List<T> elements) {
		return elements.stream().reduce((first, second) -> second).orElse(null);
	}
}

package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.helper.ProcessAnalyzerHelper;
import com.axonivy.utils.process.analyzer.internal.model.AnalysisPath;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;
import com.axonivy.utils.process.analyzer.model.DetectedAlternative;
import com.axonivy.utils.process.analyzer.model.DetectedElement;
import com.axonivy.utils.process.analyzer.model.DetectedTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.TaskState;

@SuppressWarnings("restriction")
public abstract class ProcessAnalyzer {
	private static final List<TaskState> OPEN_TASK_STATES = List.of(TaskState.SUSPENDED, TaskState.PARKED, TaskState.RESUMED);
	private ProcessGraph processGraph;
	
	protected ProcessAnalyzer() {
		this.processGraph = new ProcessGraph();
	}

	protected abstract Map<String, Duration> getDurationOverrides();
	protected abstract Map<String, String> getProcessFlowOverrides();
	protected abstract boolean isDescribeAlternativeElements();

	protected Map<ProcessElement, List<AnalysisPath>> findPath(ProcessElement... from) throws Exception {
		WorkflowPath workflowPath = new WorkflowPath(getProcessFlowOverrides());
		Map<ProcessElement, List<AnalysisPath>> paths = workflowPath.findPath(from);
		return paths;
	}
	
	protected Map<ProcessElement, List<AnalysisPath>> findPath(String flowName, ProcessElement... from) throws Exception {
		WorkflowPath workflowPath = new WorkflowPath(getProcessFlowOverrides());
		Map<ProcessElement, List<AnalysisPath>> paths = workflowPath.findPath(flowName, from);
		return paths;
	}
	
	protected Duration calculateTotalDuration(Map<ProcessElement, List<AnalysisPath>> paths, Enum<?> useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		Duration total = workflowTime.calculateTotalDuration(paths, useCase);
		return total;
	}
	
	protected List<DetectedElement> convertToDetectedElements(Map<ProcessElement, List<AnalysisPath>> paths, Enum<?> useCase) {
		Map<ProcessElement, Duration> timeUntilStartAts = paths.keySet().stream().collect(Collectors.toMap(it ->it, it -> Duration.ZERO));
		List<DetectedElement> result = convertToDetectedElements(paths, useCase, timeUntilStartAts);
		return result;
	}
	
	protected Map<ProcessElement, Duration> getProcessElementWithStartTimestamp(List<ITask> tasks) {
		Map<ProcessElement, Duration> result = new LinkedHashMap<>();
		for(ITask task : tasks) {
			BaseElement element = ProcessAnalyzerHelper.getBaseElementOf(task);
			result.put(new CommonElement(element), Duration.ZERO);
		}
		
		return result;
	}
	
	protected List<ITask> getCaseITasks(ICase icase) {
		List<ITask> tasks = icase.tasks().all().stream().filter(task -> OPEN_TASK_STATES.contains(task.getState())).toList();
		return tasks;
	}
	
	protected List<DetectedElement> convertToDetectedElements(Map<ProcessElement, List<AnalysisPath>> allPaths, Enum<?> useCase, Map<ProcessElement, Duration> timeUntilStarts) {
		List<DetectedElement> result = new ArrayList<>();
		for (Entry<ProcessElement, List<AnalysisPath>> path : allPaths.entrySet()) {
			List<DetectedElement> elements = convertPathToDetectedElements(path.getKey(), path.getValue(), useCase, timeUntilStarts);
			result.addAll(elements);
		}
		result = keepMaxTimeUtilEndDetectedElement(result);
		return result;
	}
	
	private List<DetectedElement> convertPathToDetectedElements(ProcessElement startElement, List<AnalysisPath> paths, Enum<?> useCase, Map<ProcessElement, Duration> timeUntilStarts) {
		List<List<DetectedElement>> allPaths = new ArrayList<>();
		paths.forEach(path -> {
			allPaths.add(convertPathToDetectedElements(startElement, path, useCase, timeUntilStarts));
		});
		List<DetectedElement> result = allPaths.stream().flatMap(List::stream).toList();
		return result;
	}
	
	private List<DetectedElement> convertPathToDetectedElements(ProcessElement startElement, AnalysisPath path, Enum<?> useCase, Map<ProcessElement, Duration> timeUntilStarts) {
		List<DetectedElement> result = new ArrayList<>();
		Duration timeUntilStart = timeUntilStarts.get(startElement);
		Duration durationStart = timeUntilEnd(result, timeUntilStart);

		for (ProcessElement element : path.getElements()) {
			// CommonElement(RequestStart)
			if (processGraph.isRequestStart(element.getElement())) {
				continue;
			}

			if (processGraph.isTaskAndCaseModifier(element.getElement()) && processGraph.isSystemTask(element.getElement())) {
				continue;
			}

			// CommonElement(Alternative)
			if (processGraph.isAlternative(element.getElement()) && this.isDescribeAlternativeElements()) {
				var delectedAlternative = createDetectedAlternative(element);
				if (delectedAlternative != null) {
					result.add(delectedAlternative);
				}
			}

			// CommonElement(SingleTaskCreator)
			if (processGraph.isSingleTaskCreator(element.getElement())) {
				SingleTaskCreator singleTask = (SingleTaskCreator) element.getElement();
				var detectedTask = createDetectedTask(singleTask, singleTask.getTaskConfig(), durationStart, useCase);
				if (detectedTask != null) {
					result.add(detectedTask);
					durationStart = timeUntilEnd(result, timeUntilStart);
				}
				continue;
			}

			if (element instanceof TaskParallelGroup) {
				var startedForGroup = element.getElement() == null ? timeUntilStarts : Map.of(element, durationStart);
				
				TaskParallelGroup group = (TaskParallelGroup) element;
				var tasks = convertTaskParallelGroupToDetectedElement(group, useCase, startedForGroup);				
				if (isNotEmpty(tasks)) {
					result.addAll(tasks);
					durationStart = getMaxDurationUntilEnd(tasks);
				}
				continue;
			}

			// CommonElement(SingleTaskCreator)
			if (processGraph.isSingleTaskCreator(element.getElement())) {
				SingleTaskCreator singleTask = (SingleTaskCreator) element.getElement();
				var detectedTask = createDetectedTask(singleTask, singleTask.getTaskConfig(), durationStart, useCase);
				if (detectedTask != null) {
					result.add(detectedTask);
					durationStart = timeUntilEnd(result, durationStart);
				}
				continue;
			}

			if (element instanceof CommonElement && processGraph.isSequenceFlow(element.getElement())) {
				SequenceFlow sequenceFlow = (SequenceFlow) element.getElement();
				if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
					var startTask = createStartTaskFromTaskSwitchGateway(sequenceFlow, durationStart, useCase);
					if (startTask != null) {
						result.add(startTask);
						durationStart = timeUntilEnd(result, timeUntilStart);
					}
					continue;
				}
			}
		}
		return result;
	}
	
	private List<DetectedElement> convertTaskParallelGroupToDetectedElement(TaskParallelGroup group, Enum<?> useCase, Map<ProcessElement, Duration> timeUntilStartAts) {		
		List<DetectedElement> tasksWithTaskEnd = convertTaskParallelGroupToDetectedElement(group, useCase, timeUntilStartAts, true);
		List<DetectedElement> tasksInternal = convertTaskParallelGroupToDetectedElement(group, useCase, timeUntilStartAts, false);		
		return ListUtils.union(tasksWithTaskEnd, tasksInternal);
	}
	
	private List<DetectedElement> convertTaskParallelGroupToDetectedElement(TaskParallelGroup group, Enum<?> useCase, Map<ProcessElement, Duration> timeUntilStartAts, boolean withTaskEnd) {	
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		Map<SequenceFlow, List<AnalysisPath>> internalPath =  workflowTime.getInternalPath(group.getInternalPaths(), withTaskEnd);
		
		List<DetectedElement> result = new ArrayList<>();
		for (Entry<SequenceFlow, List<AnalysisPath>> entry : internalPath.entrySet()) {
			SequenceFlow sequenceFlow = (SequenceFlow)entry.getKey();
			Duration startedAt = timeUntilStartAts.get(group);; 
			
			if(group.getElement() != null) {
				var startTask = createStartTaskFromTaskSwitchGateway(sequenceFlow, startedAt, useCase);
				result.add(startTask);
				startedAt = ((DetectedTask)startTask).getTimeUntilEnd();
			} 
			
			ProcessElement key = new CommonElement(sequenceFlow);
			Map<ProcessElement, List<AnalysisPath>> path = Map.of(key, entry.getValue());
			if(startedAt == null) {
				startedAt = timeUntilStartAts.get(new CommonElement(sequenceFlow.getTarget()));
			}
				
			var tasks = convertToDetectedElements(path, useCase, Map.of(key, startedAt));
			result.addAll(tasks);
		}
		
		return result;
	}
	
	private Duration timeUntilEnd(List<DetectedElement> detectedElements, Duration defaultAt) {
		List<DetectedTask> detectedTasks = detectedElements.stream()
				.filter(item -> item instanceof DetectedTask)
				.map(DetectedTask.class::cast)
				.toList();
		int size =  detectedTasks.size();
		return size > 0 ? detectedTasks.get(size - 1).getTimeUntilEnd() : defaultAt;
	}
	
	private Duration getMaxDurationUntilEnd(List<DetectedElement> detectedElements) {
		Duration maxDurationUntilEnd = detectedElements.stream()
				.filter(item -> item instanceof DetectedTask == true)
				.map(DetectedTask.class::cast)
				.map(DetectedTask::getTimeUntilEnd)				
				.max(Duration::compareTo)
				.orElse(null);
				
		return maxDurationUntilEnd;
	}
	
	private DetectedElement createStartTaskFromTaskSwitchGateway(SequenceFlow sequenceFlow, Duration timeUntilStartedAt, Enum<?> useCase) {

		DetectedElement task = null;
		if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
			TaskSwitchGateway taskSwitchGateway = (TaskSwitchGateway) sequenceFlow.getSource();
			if (!processGraph.isSystemTask(taskSwitchGateway)) {
				TaskConfig startTask = processGraph.getStartTaskConfig(sequenceFlow);
				task = createDetectedTask((TaskAndCaseModifier) taskSwitchGateway, startTask, timeUntilStartedAt, useCase);
			}
		}
		return task;
	}
	
	private DetectedAlternative createDetectedAlternative(ProcessElement processElement) {
		if(processGraph.isAlternative(processElement.getElement())) {
			Alternative alternative = (Alternative) processElement.getElement();
			DetectedAlternative result = new DetectedAlternative();
			result.setTaskName(alternative.getName());
			result.setPid(alternative.getPid().getRawPid());
			List<DetectedElement> options = alternative.getOutgoing()
					.stream()
					.map(item -> convertToDetectedElement(item))
					.toList();
			result.setOptions(options);
			return result;
		}
		return null;
	}

	private DetectedElement convertToDetectedElement(SequenceFlow outcome) {
		DetectedElement element = new DetectedElement() {};
		element.setPid(outcome.getPid().getRawPid());
		String name = outcome.getName();
		element.setElementName(name.equals(StringUtils.EMPTY) ? "No name" : name);
		return element;
	}
	
	private DetectedElement createDetectedTask(TaskAndCaseModifier task, TaskConfig taskConfig, Duration timeUntilStartAt, Enum<?> useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		DetectedTask detectedTask = new DetectedTask();
		
		detectedTask.setPid(processGraph.getTaskId(task, taskConfig));		
		detectedTask.setParentElementNames(getParentElementNames(task));
		detectedTask.setTaskName(taskConfig.getName().getRawMacro());
		detectedTask.setElementName(task.getName());
		Duration estimatedDuration = workflowTime.getDuration(task, taskConfig, useCase);				
		detectedTask.setEstimatedDuration(estimatedDuration);
		detectedTask.setTimeUntilStart(timeUntilStartAt);		
		detectedTask.setTimeUntilEnd(timeUntilStartAt.plus(estimatedDuration));
		String customerInfo = getCustomInfoByCode(taskConfig);
		detectedTask.setCustomInfo(customerInfo);		
		
		return detectedTask;
	}
	
	private List<String> getParentElementNames(TaskAndCaseModifier task){
		List<String> parentElementNames = emptyList();
		if(task.getParent() instanceof EmbeddedProcessElement) {
			parentElementNames = processGraph.getParentElementNamesEmbeddedProcessElement(task.getParent());
		}		
		return parentElementNames ;
	}
	
	private String getCustomInfoByCode(TaskConfig task) {
		String wfEstimateCode = processGraph.getCodeLineByPrefix(task, "APAConfig.setCustomInfo");		
		String result = Optional.ofNullable(wfEstimateCode)
				.filter(StringUtils::isNotEmpty)
				.map(it -> StringUtils.substringBetween(it, "(\"", "\")"))
				.orElse(null);;
		
		return result;
	}
	
	private Map<ProcessElement, List<ProcessElement>> removeDuplicate(Map<ProcessElement, List<ProcessElement>> paths) {
		Map<ProcessElement, List<ProcessElement>> result = new LinkedHashMap<>();
		paths.entrySet().forEach(it -> {
			result.put(it.getKey(), it.getValue().stream().distinct().toList());
		});

		return result;
	}
	
	protected List<DetectedElement> keepMaxTimeUtilEndDetectedElement(List<DetectedElement> detectedElements) {
		Map<String, Duration> taskGroup = new HashedMap<>();
		for (DetectedElement detectedElement : detectedElements) {
			if (detectedElement instanceof DetectedTask) {
				Duration duration = ((DetectedTask) detectedElement).getTimeUntilEnd();
				Duration maxDuration = taskGroup.getOrDefault(detectedElement.getPid(), Duration.ZERO);
				maxDuration = maxDuration.compareTo(duration) > 0 ? maxDuration : duration;
				taskGroup.put(detectedElement.getPid(), maxDuration);
			}
		}
		
		List<DetectedElement> result = new ArrayList<>();
		
		for (DetectedElement detectedElement : detectedElements) {
			if (detectedElement instanceof DetectedAlternative && !containsByPid(result, detectedElement)) {
				result.add(detectedElement);
			}
			
			if (detectedElement instanceof DetectedTask && !containsByPid(result, detectedElement)) {
				Duration maxDuration = taskGroup.getOrDefault(detectedElement.getPid(), Duration.ZERO);
				Duration duration = ((DetectedTask) detectedElement).getTimeUntilEnd();
				if (duration.compareTo(maxDuration) >= 0) {
					result.add(detectedElement);
				}
			}
		}
		
		return result;
	}
	
	private boolean containsByPid(List<DetectedElement> detectedElements, DetectedElement detectedElement) {
		List<String> pids = detectedElements.stream().map(DetectedElement::getPid).toList();
		return pids.contains(detectedElement.getPid());
	}
}

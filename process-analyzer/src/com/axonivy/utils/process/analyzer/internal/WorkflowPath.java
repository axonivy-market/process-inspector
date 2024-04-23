package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.internal.model.AnalysisPath;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;
import com.axonivy.utils.process.analyzer.model.DetectedAlternative;
import com.axonivy.utils.process.analyzer.model.DetectedElement;
import com.axonivy.utils.process.analyzer.model.DetectedTask;
import com.axonivy.utils.process.analyzer.model.ElementTask;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

class WorkflowPath {
	private static final Duration DURATION_MIN = Duration.ofMillis(Long.MIN_VALUE);
	private ProcessGraph processGraph;
	
	private String flowName;
	private List<ProcessElement> froms;	
	
	private boolean isEnableDescribeAlternative;
	private Map<String, String> processFlowOverrides = emptyMap();
	private Map<ElementTask, Duration> durationOverrides = emptyMap();
	
	protected WorkflowPath() {
		this.processGraph = new ProcessGraph();
	}
	
	protected WorkflowPath setProcessFlowOverrides(Map<String, String> processFlowOverrides) {
		this.processFlowOverrides = processFlowOverrides;
		return this;
	}
	
	protected WorkflowPath setDurationOverrides(Map<ElementTask, Duration> durationOverrides) {
		this.durationOverrides = durationOverrides;
		return this;
	}
	
	protected WorkflowPath setIsEnableDescribeAlternative(boolean isEnableDescribeAlternative) {
		this.isEnableDescribeAlternative = isEnableDescribeAlternative;
		return this;
	}
	
	protected List<DetectedElement> findAllTasks(List<ProcessElement> froms, Enum<?> useCase, Map<ProcessElement, Duration> timeUntilStarts) throws Exception {		
		Map<ProcessElement, List<AnalysisPath>> paths = new PathFinder()
				.setProcessFlowOverrides(this.processFlowOverrides)
				.setStartElements(froms)
				.findAllTask();
		
		List<DetectedElement> detectedTasks = convertToDetectedElements(paths, useCase, timeUntilStarts);		
		return detectedTasks;
	}
	
	protected List<DetectedElement> findTaskOnPath(List<ProcessElement> froms, Enum<?> useCase, String flowName, Map<ProcessElement, Duration> timeUntilStarts) throws Exception {		
		Map<ProcessElement, List<AnalysisPath>> paths = new PathFinder()
				.setProcessFlowOverrides(this.processFlowOverrides)
				.setFlowName(flowName)
				.setStartElements(froms)
				.findTaskOnPath();
		
		List<DetectedElement> detectedTasks = convertToDetectedElements(paths, useCase, timeUntilStarts);		
		return detectedTasks;
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
			if (processGraph.isAlternative(element.getElement()) && this.isEnableDescribeAlternative) {
				var delectedAlternative = createDetectedAlternative(element);
				if (delectedAlternative != null) {
					result.add(delectedAlternative);
				}
			}
			
			// Sub process call
			if(processGraph.isSubProcessCall(element.getElement())) {
				SubProcessCall subProcessCall = (SubProcessCall)element.getElement();
				if(processGraph.isHandledAsTask(subProcessCall)) {
					var detectedSubProcessCall = createDetectedTaskFromSubProcessCall(subProcessCall, useCase, durationStart);
					if (detectedSubProcessCall != null) {
						result.add(detectedSubProcessCall);
						durationStart = timeUntilEnd(result, timeUntilStart);
					}
				}				
			}

			// CommonElement(SingleTaskCreator)
			if (processGraph.isSingleTaskCreator(element.getElement())) {
				SingleTaskCreator singleTask = (SingleTaskCreator) element.getElement();
				var detectedTask = createDetectedTask(singleTask, singleTask.getTaskConfig(), useCase, durationStart);
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
				var detectedTask = createDetectedTask(singleTask, singleTask.getTaskConfig(), useCase, durationStart);
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
		Map<SequenceFlow, List<AnalysisPath>> internalPath = getInternalPath(group.getInternalPaths(), withTaskEnd);
		 		
		List<DetectedElement> result = new ArrayList<>();
		for (Entry<SequenceFlow, List<AnalysisPath>> entry : internalPath.entrySet()) {
			SequenceFlow sequenceFlow = (SequenceFlow)entry.getKey();
			Duration startedAt = timeUntilStartAts.get(group); 
			
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
				task = createDetectedTask((TaskAndCaseModifier) taskSwitchGateway, startTask, useCase, timeUntilStartedAt);
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
	
	private DetectedElement createDetectedTaskFromSubProcessCall(SubProcessCall subProcessCall, Enum<?> useCase, Duration timeUntilStartAt) {
		DetectedTask detectedTask = new DetectedTask();
		detectedTask.setPid(subProcessCall.getPid().getRawPid());		
	
		String script = subProcessCall.getParameters().getCode();
		String taskName = getTaskNameByCode(script);
		detectedTask.setTaskName(taskName);
		detectedTask.setElementName(subProcessCall.getName());
		
		Duration estimatedDuration =  new WorkflowDuration()
				.setDurationOverrides(this.durationOverrides)
				.getDurationByTaskScript(script, useCase);
		
		detectedTask.setEstimatedDuration(estimatedDuration);
		detectedTask.setTimeUntilStart(timeUntilStartAt);		
		detectedTask.setParentElementNames(emptyList());
		detectedTask.setTimeUntilEnd(timeUntilStartAt.plus(estimatedDuration));
		
		String customerInfo = getCustomInfoByCode(script);
		detectedTask.setCustomInfo(customerInfo);		
		
		return detectedTask;
	}

	private DetectedElement convertToDetectedElement(SequenceFlow outcome) {
		DetectedElement element = new DetectedElement() {};
		element.setPid(outcome.getPid().getRawPid());
		String name = outcome.getName();
		element.setElementName(name.equals(StringUtils.EMPTY) ? "No name" : name);
		return element;
	}
	
	private DetectedElement createDetectedTask(TaskAndCaseModifier task, TaskConfig taskConfig,Enum<?> useCase, Duration timeUntilStartAt) {		
		DetectedTask detectedTask = new DetectedTask();
		
		detectedTask.setPid(processGraph.getTaskId(task, taskConfig).getId());		
		detectedTask.setParentElementNames(getParentElementNames(task));
		detectedTask.setTaskName(taskConfig.getName().getRawMacro());
		detectedTask.setElementName(task.getName());		
		detectedTask.setTimeUntilStart(timeUntilStartAt);
		
		Duration estimatedDuration = new WorkflowDuration()
				.setDurationOverrides(this.durationOverrides)
				.getDuration(task, taskConfig, useCase);
			
		detectedTask.setEstimatedDuration(estimatedDuration);		
		detectedTask.setTimeUntilEnd(timeUntilStartAt.plus(estimatedDuration));
		
		String customerInfo = getCustomInfoByCode(taskConfig);
		detectedTask.setCustomInfo(customerInfo);		
		
		return detectedTask;
	}
	
	private List<DetectedElement> keepMaxTimeUtilEndDetectedElement(List<DetectedElement> detectedElements) {
		Map<String, Duration> taskGroup = new LinkedHashMap<>();
		for (DetectedElement detectedElement : detectedElements) {
			if (detectedElement instanceof DetectedTask) {
				Duration duration = ((DetectedTask) detectedElement).getTimeUntilEnd();
				Duration maxDuration = taskGroup.getOrDefault(detectedElement.getPid(), DURATION_MIN);
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
				Duration maxDuration = taskGroup.getOrDefault(detectedElement.getPid(), DURATION_MIN);
				Duration duration = ((DetectedTask) detectedElement).getTimeUntilEnd();
				if (duration.compareTo(maxDuration) >= 0) {
					result.add(detectedElement);
				}
			}
		}
		
		return result;
	}
	
	private List<String> getParentElementNames(TaskAndCaseModifier task){
		List<String> parentElementNames = emptyList();
		if(task.getParent() instanceof EmbeddedProcessElement) {
			parentElementNames = processGraph.getParentElementNamesEmbeddedProcessElement(task.getParent());
		}		
		return parentElementNames ;
	}
	private String getCustomInfoByCode(TaskConfig task) {
		return getCustomInfoByCode(task.getScript());
	}
	
	private String getCustomInfoByCode(String script) {
		String wfEstimateCode = processGraph.getCodeLineByPrefix(script, "APAConfig.setCustomInfo");		
		String result = Optional.ofNullable(wfEstimateCode)
				.filter(StringUtils::isNotEmpty)
				.map(it -> StringUtils.substringBetween(it, "(\"", "\")"))
				.orElse(null);
		
		return result;
	}
	
	private String getTaskNameByCode(String script) {
		String wfEstimateCode = processGraph.getCodeLineByPrefix(script, "APAConfig.setTaskName");		
		String result = Optional.ofNullable(wfEstimateCode)
				.filter(StringUtils::isNotEmpty)
				.map(it -> StringUtils.substringBetween(it, "(\"", "\")"))
				.orElse(null);
		
		return result;
	}
	
	private boolean containsByPid(List<DetectedElement> detectedElements, DetectedElement detectedElement) {
		List<String> pids = detectedElements.stream().map(DetectedElement::getPid).toList();
		return pids.contains(detectedElement.getPid());
	}
	
	public Map<SequenceFlow, List<AnalysisPath>> getInternalPath(Map<SequenceFlow, List<AnalysisPath>> internalPath,
			boolean withTaskEnd) {
		Map<SequenceFlow, List<AnalysisPath>> paths = new LinkedHashMap<>();

		// Priority the path go to end first
		for (SequenceFlow sf : internalPath.keySet()) {
			List<AnalysisPath> analysisPaths = new ArrayList<>();
			for (AnalysisPath path : internalPath.get(sf)) {
				ProcessElement last = getLast(path.getElements());
				if (withTaskEnd && last.getElement() instanceof TaskEnd) {
					analysisPaths.add(path);
				} else if (!withTaskEnd && last.getElement() instanceof TaskEnd == false) {
					analysisPaths.add(path);
				}
			}
			if (isNotEmpty(analysisPaths)) {
				paths.put(sf, analysisPaths);
			}
		}

		return paths;
	}
	
	private <T> T getLast(List<T> elements) {
		return elements.stream().reduce((first, second) -> second).orElse(null);
	}
}

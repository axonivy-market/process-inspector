package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.helper.ProcessAnalyzerHelper;
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

	protected Map<ProcessElement, List<ProcessElement>> findPath(ProcessElement... from) throws Exception {
		WorkflowPath workflowPath = new WorkflowPath(getProcessFlowOverrides());
		Map<ProcessElement, List<ProcessElement>> paths = workflowPath.findPath(from);
		//Remove duplicate for find all tasks case
		return removeDuplicate(paths);
	}
	
	protected Map<ProcessElement, List<ProcessElement>> findPath(String flowName, ProcessElement... from) throws Exception {
		WorkflowPath workflowPath = new WorkflowPath(getProcessFlowOverrides());
		Map<ProcessElement, List<ProcessElement>> paths = workflowPath.findPath(flowName, from);
		return paths;
	}
	
	protected Duration calculateTotalDuration(Map<ProcessElement, List<ProcessElement>> path, Enum<?> useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		return workflowTime.calculateTotalDuration(path, useCase);
	}
	
	protected List<DetectedElement> convertToDetectedElements(Map<ProcessElement, List<ProcessElement>> paths, Enum<?> useCase) {		
		Map<ProcessElement, List<ProcessElement>> distinctedPath = mergePath(paths);
		Map<ProcessElement, Date> startAts = distinctedPath.keySet().stream().collect(Collectors.toMap(it ->it, it -> new Date()));
		List<DetectedElement> result = convertToDetectedElements(distinctedPath, useCase, startAts);
		return result;
	}
	
	protected Map<ProcessElement, Date> getProcessElementWithStartTimestamp(List<ITask> tasks) {
		Map<ProcessElement, Date> result = new LinkedHashMap<>();
		for(ITask task : tasks) {
			BaseElement element = ProcessAnalyzerHelper.getBaseElementOf(task);
			result.put(new CommonElement(element), task.getStartTimestamp());
		}
		
		return result;
	}
	
	protected List<ITask> getCaseITasks(ICase icase) {
		List<ITask> tasks = icase.tasks().all().stream().filter(task -> OPEN_TASK_STATES.contains(task.getState())).toList();
		return tasks;
	}
	
	private Map<ProcessElement, List<ProcessElement>> mergePath(Map<ProcessElement, List<ProcessElement>> path) {
		ProcessElement key = path.keySet().stream().findFirst().get();
		List<ProcessElement> elements = path.values().stream().flatMap(List::stream).distinct().toList();
		return Map.of(key, elements);
	}
	
	protected List<DetectedElement> convertToDetectedElements(Map<ProcessElement, List<ProcessElement>> paths, Enum<?> useCase, Map<ProcessElement, Date> startedAts) {

		// convert to both detected task and alternative
		List<DetectedElement> result = new ArrayList<>();	
		for(Entry<ProcessElement, List<ProcessElement>> path : paths.entrySet()) {
			Date startedAt = startedAts.get(path.getKey());
			Date startAtTime = getEndTimestamp(result, startedAt);
			for(ProcessElement element : path.getValue()) {
				
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
					var detectedTask = createDetectedTask(singleTask, singleTask.getTaskConfig(), startAtTime, useCase);
					if (detectedTask != null) {
						result.add(detectedTask);
						startAtTime = getEndTimestamp(result, startedAt);
					}
					continue;
				}
			
				if (element instanceof TaskParallelGroup) {
					var startedForGroup = element.getElement() == null ? startedAts : Map.of(element, startAtTime);
					var tasks = convertToDetectedElementFromTaskParallelGroup((TaskParallelGroup) element, useCase, startedForGroup);
					if (isNotEmpty(tasks)) {
						result.addAll(tasks);
						startAtTime = getMaxEndTimestamp(tasks);
					}
					continue;
				}
				
				// CommonElement(SingleTaskCreator)
				if (processGraph.isSingleTaskCreator(element.getElement())) {
					SingleTaskCreator singleTask = (SingleTaskCreator) element.getElement();
					var detectedTask = createDetectedTask(singleTask, singleTask.getTaskConfig(), startAtTime, useCase);
					if (detectedTask != null) {
						result.add(detectedTask);
						startAtTime = getEndTimestamp(result, startedAt);
					}
					continue;
				}
				
				if (element instanceof CommonElement && processGraph.isSequenceFlow(element.getElement())) {
					SequenceFlow sequenceFlow = (SequenceFlow) element.getElement();
					if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
						var startTask = createStartTaskFromTaskSwitchGateway(sequenceFlow, startAtTime, useCase);
						if (startTask != null) {
							result.add(startTask);
							startAtTime = getEndTimestamp(result, startedAt);
						}
						continue;
					}
				}
			}
		}
		
		return result.stream().filter(item -> item != null).toList();		
	}
	
	private List<DetectedElement> convertToDetectedElementFromTaskParallelGroup(TaskParallelGroup group, Enum<?> useCase, Map<ProcessElement, Date> startedAts) {	
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		Map<SequenceFlow, List<ProcessElement>> sortedInternalPath =  new LinkedHashMap<>();
		sortedInternalPath.putAll(workflowTime.getInternalPath(group.getInternalPaths(), true));
		sortedInternalPath.putAll(workflowTime.getInternalPath(group.getInternalPaths(), false));
		
		List<DetectedElement> result = new ArrayList<>();
		for (Entry<SequenceFlow, List<ProcessElement>> entry : sortedInternalPath.entrySet()) {
			SequenceFlow sequenceFlow = (SequenceFlow)entry.getKey();
			Date startedAt = startedAts.get(group);; 
			if(group.getElement() != null) {
				var startTask = createStartTaskFromTaskSwitchGateway(sequenceFlow, startedAt, useCase);
				result.add(startTask);
				startedAt = ((DetectedTask)startTask).calculateEstimatedEndTimestamp();
			} 
			
			ProcessElement key = new CommonElement(sequenceFlow);
			Map<ProcessElement, List<ProcessElement>> path = Map.of(key, entry.getValue());
			if(startedAt == null) {
				startedAt = startedAts.get(new CommonElement(sequenceFlow.getTarget()));
			}
			
						
			var tasks = convertToDetectedElements(path, useCase, Map.of(key, startedAt));
			result.addAll(tasks);
		}
		
		return result;
	}
		
	private Date getEndTimestamp(List<DetectedElement> detectedElements, Date defaultAt) {
		List<DetectedTask> detectedTasks = detectedElements.stream()
				.filter(item -> item instanceof DetectedTask)
				.map(DetectedTask.class::cast)
				.toList();
		int size =  detectedTasks.size();
		return size > 0 ? detectedTasks.get(size - 1).calculateEstimatedEndTimestamp() : defaultAt;
	}
	
	private Date getMaxEndTimestamp(List<DetectedElement> detectedElements) {
		Date maxEndTimeStamp = detectedElements.stream()
				.filter(item -> item instanceof DetectedTask == true)
				.map(DetectedTask.class::cast)
				.map(DetectedTask::calculateEstimatedEndTimestamp)				
				.max(Date::compareTo)
				.orElse(null);
				
		return maxEndTimeStamp;
	}
	
	private DetectedElement createStartTaskFromTaskSwitchGateway(SequenceFlow sequenceFlow, Date startedAt, Enum<?> useCase) {

		DetectedElement task = null;
		if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
			TaskSwitchGateway taskSwitchGateway = (TaskSwitchGateway) sequenceFlow.getSource();
			if (!processGraph.isSystemTask(taskSwitchGateway)) {
				TaskConfig startTask = processGraph.getStartTaskConfig(sequenceFlow);
				task = createDetectedTask((TaskAndCaseModifier) taskSwitchGateway, startTask, startedAt, useCase);
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
	
	private DetectedElement createDetectedTask(TaskAndCaseModifier task, TaskConfig taskConfig, Date startedAt, Enum<?> useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		DetectedTask detectedTask = new DetectedTask();
		
		detectedTask.setPid(processGraph.getTaskId(task, taskConfig));		
		detectedTask.setParentElementNames(getParentElementNames(task));
		detectedTask.setTaskName(taskConfig.getName().getRawMacro());
		detectedTask.setElementName(task.getName());
		Duration estimatedDuration = workflowTime.getDuration(task, taskConfig, useCase);				
		detectedTask.setEstimatedDuration(estimatedDuration);
		detectedTask.setEstimatedStartTimestamp(startedAt);		
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
}

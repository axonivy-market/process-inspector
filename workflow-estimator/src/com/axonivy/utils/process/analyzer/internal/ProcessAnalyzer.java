package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.process.analyzer.constant.UseCase;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.internal.model.TaskParallelGroup;
import com.axonivy.utils.process.analyzer.model.DetectedElement;
import com.axonivy.utils.process.analyzer.model.DetectedTask;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public abstract class ProcessAnalyzer {

	protected ProcessGraph processGraph;
	
	protected ProcessAnalyzer() {
		this.processGraph = new ProcessGraph();
	}

	protected abstract Map<String, Duration> getDurationOverrides();
	protected abstract Map<String, String> getProcessFlowOverrides();

	protected List<ProcessElement> findPath(ProcessElement... from) throws Exception {
		WorkflowPath workflowPath = new WorkflowPath(getProcessFlowOverrides());
		List<ProcessElement> path = workflowPath.findPath(from);		
		return path.stream().distinct().toList();
	}
	
	protected List<ProcessElement> findPath(String flowName, ProcessElement... from) throws Exception {
		WorkflowPath workflowPath = new WorkflowPath(getProcessFlowOverrides());
		List<ProcessElement> path = workflowPath.findPath(flowName, from);
		return path;
	}
	
	protected Duration calculateTotalDuration(List<ProcessElement> path, UseCase useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		return workflowTime.calculateTotalDuration(path, useCase);
	}
	
	protected boolean isSystemTask(TaskAndCaseModifier task) {		
		return task.getAllTaskConfigs().stream().anyMatch(it -> "SYSTEM".equals(it.getActivator().getName()));
	}
	
	
	protected TaskConfig getStartTaskConfigFromTaskSwitchGateway(SequenceFlow sequenceFlow) {
		return processGraph.getStartTaskConfig(sequenceFlow);
	}
	
	protected List<DetectedElement> convertToEstimatedElements(List<ProcessElement> path, UseCase useCase) {
		List<DetectedElement> result = convertToEstimatedElements(path, useCase, new Date());
		return result;
	}
	
	private List<DetectedElement> convertToEstimatedElements(List<ProcessElement> path, UseCase useCase, Date startedAt) {

		// convert to both Estimated Task and alternative
		List<DetectedElement> result = new ArrayList<>();		
		
		for(int i = 0; i < path.size(); i++) {
			Date startAtTime = getEstimatedEndTimestamp(result, startedAt);
			ProcessElement element = path.get(i);
		
			// CommonElement(RequestStart)
			if (element.getElement() instanceof RequestStart) {
				continue;
			}
			
			if (element.getElement()instanceof TaskAndCaseModifier && isSystemTask((TaskAndCaseModifier) element.getElement())) {
				continue;
			}
			
			if (element instanceof TaskParallelGroup) {
				var tasks = convertToEstimatedElementFromTaskParallelGroup((TaskParallelGroup) element, useCase, startAtTime);
				result.addAll(tasks);
				continue;
			}
			
			// CommonElement(SingleTaskCreator)
			if (element.getElement() instanceof SingleTaskCreator) {
				SingleTaskCreator singleTask = (SingleTaskCreator)element.getElement();
				var estimatedTask = createEstimatedTask(singleTask, singleTask.getTaskConfig(), startAtTime, useCase);
				result.add(estimatedTask);
				continue;
			}
			
			if (element instanceof CommonElement && element.getElement() instanceof SequenceFlow) {
				SequenceFlow sequenceFlow = (SequenceFlow) element.getElement();
				if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
					var startTask = createStartTaskFromTaskSwitchGateway(sequenceFlow, startAtTime, useCase);
					result.add(startTask);
					continue;
				}
			}
		}
		
		return result.stream().filter(item -> item != null).toList();		
	}
	
	private List<DetectedElement> convertToEstimatedElementFromTaskParallelGroup(TaskParallelGroup group, UseCase useCase, Date startedAt) {	
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		Map<SequenceFlow, List<ProcessElement>> sortedInternalPath =  new LinkedHashMap<>();
		sortedInternalPath.putAll(workflowTime.getInternalPath(group.getInternalPaths(), true));
		sortedInternalPath.putAll(workflowTime.getInternalPath(group.getInternalPaths(), false));
		
		List<DetectedElement> result = new ArrayList<>();
		for (Entry<SequenceFlow, List<ProcessElement>> entry : sortedInternalPath.entrySet()) {
			var startTask = createStartTaskFromTaskSwitchGateway(entry.getKey(), startedAt, useCase);
			var tasks = convertToEstimatedElements(entry.getValue(), useCase, ((DetectedTask)startTask).calculateEstimatedEndTimestamp());
			
			result.add(startTask);
			result.addAll(tasks);
		}
		
		return result;
	}
		
	private Date getEstimatedEndTimestamp(List<DetectedElement> estimatedElements, Date defaultAt) {
		List<DetectedTask> estimatedTasks = estimatedElements.stream()
				.filter(item -> item instanceof DetectedTask)
				.map(DetectedTask.class::cast)
				.toList();
		int size =  estimatedTasks.size();
		return size > 0 ? estimatedTasks.get(size - 1).calculateEstimatedEndTimestamp() : defaultAt;
	}
	
	private DetectedElement createStartTaskFromTaskSwitchGateway(SequenceFlow sequenceFlow, Date startedAt, UseCase useCase) {

		DetectedElement task = null;
		if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
			TaskSwitchGateway taskSwitchGateway = (TaskSwitchGateway) sequenceFlow.getSource();
			if (!isSystemTask(taskSwitchGateway)) {
				TaskConfig startTask = getStartTaskConfigFromTaskSwitchGateway(sequenceFlow);
				task = createEstimatedTask((TaskAndCaseModifier) taskSwitchGateway, startTask, startedAt, useCase);
			}
		}
		return task;
	}
	
	private DetectedElement createEstimatedTask(TaskAndCaseModifier task, TaskConfig taskConfig, Date startedAt, UseCase useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		DetectedTask estimatedTask = new DetectedTask();
		
		estimatedTask.setPid(processGraph.getTaskId(task, taskConfig));		
		estimatedTask.setParentElementNames(getParentElementNames(task));
		estimatedTask.setTaskName(taskConfig.getName().getRawMacro());
		estimatedTask.setElementName(task.getName());
		Duration estimatedDuration = workflowTime.getDuration(task, taskConfig, useCase);				
		estimatedTask.setEstimatedDuration(estimatedDuration);
		estimatedTask.setEstimatedStartTimestamp(startedAt);		
		String customerInfo = getCustomInfoByCode(taskConfig);
		estimatedTask.setCustomInfo(customerInfo);		
		
		return estimatedTask;
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
}

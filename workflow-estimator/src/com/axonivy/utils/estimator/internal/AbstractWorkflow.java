package com.axonivy.utils.estimator.internal;

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

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.internal.model.CommonElement;
import com.axonivy.utils.estimator.internal.model.ProcessElement;
import com.axonivy.utils.estimator.internal.model.TaskParallelGroup;
import com.axonivy.utils.estimator.model.EstimatedElement;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public abstract class AbstractWorkflow {

	protected ProcessGraph processGraph;
	
	protected AbstractWorkflow() {
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
	
	protected List<EstimatedElement> convertToEstimatedElements(List<ProcessElement> path, UseCase useCase) {
		List<EstimatedElement> result = convertToEstimatedElements(path, useCase, new Date());
		return result;
	}
	
	private List<EstimatedElement> convertToEstimatedElements(List<ProcessElement> path, UseCase useCase, Date startedAt) {

		// convert to both Estimated Task and alternative
		List<EstimatedElement> result = new ArrayList<>();		
		
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
	
	private List<EstimatedElement> convertToEstimatedElementFromTaskParallelGroup(TaskParallelGroup group, UseCase useCase, Date startedAt) {	
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		Map<SequenceFlow, List<ProcessElement>> sortedInternalPath =  new LinkedHashMap<>();
		sortedInternalPath.putAll(workflowTime.getInternalPath(group.getInternalPaths(), true));
		sortedInternalPath.putAll(workflowTime.getInternalPath(group.getInternalPaths(), false));
		
		List<EstimatedElement> result = new ArrayList<>();
		for (Entry<SequenceFlow, List<ProcessElement>> entry : sortedInternalPath.entrySet()) {
			var startTask = createStartTaskFromTaskSwitchGateway(entry.getKey(), startedAt, useCase);
			var tasks = convertToEstimatedElements(entry.getValue(), useCase, ((EstimatedTask)startTask).calculateEstimatedEndTimestamp());
			
			result.add(startTask);
			result.addAll(tasks);
		}
		
		return result;
	}
		
	private Date getEstimatedEndTimestamp(List<EstimatedElement> estimatedElements, Date defaultAt) {
		List<EstimatedTask> estimatedTasks = estimatedElements.stream()
				.filter(item -> item instanceof EstimatedTask)
				.map(EstimatedTask.class::cast)
				.toList();
		int size =  estimatedTasks.size();
		return size > 0 ? estimatedTasks.get(size - 1).calculateEstimatedEndTimestamp() : defaultAt;
	}
	
	private EstimatedElement createStartTaskFromTaskSwitchGateway(SequenceFlow sequenceFlow, Date startedAt, UseCase useCase) {

		EstimatedElement task = null;
		if (sequenceFlow.getSource() instanceof TaskSwitchGateway) {
			TaskSwitchGateway taskSwitchGateway = (TaskSwitchGateway) sequenceFlow.getSource();
			if (!isSystemTask(taskSwitchGateway)) {
				TaskConfig startTask = getStartTaskConfigFromTaskSwitchGateway(sequenceFlow);
				task = createEstimatedTask((TaskAndCaseModifier) taskSwitchGateway, startTask, startedAt, useCase);
			}
		}
		return task;
	}
	
	private EstimatedElement createEstimatedTask(TaskAndCaseModifier task, TaskConfig taskConfig, Date startedAt, UseCase useCase) {
		WorkflowTime workflowTime = new WorkflowTime(getDurationOverrides());
		EstimatedTask estimatedTask = new EstimatedTask();
		
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
		String wfEstimateCode = processGraph.getCodeLineByPrefix(task, "WfEstimate.setCustomInfo");		
		String result = Optional.ofNullable(wfEstimateCode)
				.filter(StringUtils::isNotEmpty)
				.map(it -> StringUtils.substringBetween(it, "(\"", "\")"))
				.orElse(null);;
		
		return result;
	}
}

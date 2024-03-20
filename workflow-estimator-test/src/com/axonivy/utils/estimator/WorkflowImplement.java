package com.axonivy.utils.estimator;

import static java.util.Collections.emptyMap;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.estimator.constant.UseCase;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public class WorkflowImplement extends AbstractWorkflow{

	@Override
	protected Map<String, Duration> getDurationOverrides() {
		return emptyMap();
	}

	@Override
	protected Map<String, String> getProcessFlowOverrides() {
		return emptyMap();
	}

	public List<BaseElement> findPath(String flowName, BaseElement... from) throws Exception {
		return super.findPath(flowName, from);		
	}
	
	public String getTaskId(TaskAndCaseModifier task, TaskConfig taskConfig) {
		return super.getTaskId(task, taskConfig);
	}
	
	public boolean isSystemTask(TaskAndCaseModifier task) {
		return super.isSystemTask(task);
	}
	
	public List<String> getParentElementNames(TaskAndCaseModifier task) {
		return super.getParentElementNames(task);
	}
	
	public Duration getDuration(TaskAndCaseModifier task, TaskConfig taskConfig, UseCase useCase) {
		return super.getDuration(task, taskConfig, useCase);
	}
}

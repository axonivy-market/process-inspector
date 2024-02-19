package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;

import java.util.Date;
import java.util.List;

import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;

public class WorkflowEstimator {
	
	private Process process;	
	private Enum<?> useCase;
	private String flowName;
	
	public WorkflowEstimator(Process process, Enum<?> useCase, String flowName) {
		this.process = process;
		this.useCase = useCase;
		this.flowName = flowName;
	}

	public List<EstimatedTask> findAllTasks(BaseElement startAtElement) {
		
		List<BaseElement> elements = startAtElement.getRootProcess().getElements();
		List<EstimatedTask> estimatedTasks = elements.stream()
		//filter to get task only		
		.filter(el -> {
			return el instanceof SingleTaskCreator;
		})
		//filter the tash which have estimated if needed
		.map(task -> createEstimatedTask((SingleTaskCreator) task))
		.toList();
		
		return estimatedTasks;
	}

	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) {
		// TODO: Implement here
		return emptyList();
	}
	
	private EstimatedTask createEstimatedTask(SingleTaskCreator task) {
		EstimatedTask estimatedTask = new EstimatedTask();
		estimatedTask.setPid(task.getPid().getRawPid());
		estimatedTask.setTaskName(task.getName());
		estimatedTask.setEstimatedStartTimestamp(new Date());
		
		return estimatedTask;
	}
	
}

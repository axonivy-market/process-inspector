package com.axonivy.utils.estimator.demo.model;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.axonivy.utils.estimator.demo.constant.FindType;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;

public class Estimator {
	private String id;
	private String flowName;
	private Process process;
	private List<SingleTaskCreator> elements;
	private FindType findType;
	private SingleTaskCreator startElement;
	private List<EstimatedTask> tasks;
	private Duration totalDuration;

	public Estimator() {
		this.id = UUID.randomUUID().toString();
	}

	public String getId() {
		return id;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public List<SingleTaskCreator> getElements() {
		return elements;
	}

	public void setElements(List<SingleTaskCreator> elements) {
		this.elements = elements;
	}

	public SingleTaskCreator getStartElement() {
		return startElement;
	}

	public void setStartElement(SingleTaskCreator startElement) {
		this.startElement = startElement;
	}

	public FindType getFindType() {
		return findType;
	}

	public void setFindType(FindType findType) {
		this.findType = findType;
	}

	public List<EstimatedTask> getTasks() {
		return tasks;
	}

	public void setTasks(List<EstimatedTask> tasks) {
		this.tasks = tasks;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public Duration getTotalDuration() {
		return totalDuration;
	}

	public void setTotalDuration(Duration toDuration) {
		this.totalDuration = toDuration;
	}
	
	public long getTotalDurationInHours() {
		return totalDuration.toHours();
	}

	public String getElementNames() {
		return this.tasks.stream().map(EstimatedTask::getElementName).collect(Collectors.joining(" -> "));
	}
}

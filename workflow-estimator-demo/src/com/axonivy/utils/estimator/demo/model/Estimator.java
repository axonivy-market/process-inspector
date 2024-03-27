package com.axonivy.utils.estimator.demo.model;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.demo.constant.FindType;
import com.axonivy.utils.estimator.demo.helper.DateTimeHelper;
import com.axonivy.utils.estimator.model.EstimatedElement;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;

public class Estimator {
	private String id;
	private String flowName;
	private Process process;
	private String processPath;
	private UseCase useCase;
	private List<SingleTaskCreator> elements;
	private FindType findType;
	private SingleTaskCreator startElement;
	private List<EstimatedElement> tasks;
	private Duration totalDuration;
	private long executionTime;
	private List<Alternative> alternatives;
	private Map<Alternative, SequenceFlow> alternativeFlows;

	public Estimator() {
		this.id = UUID.randomUUID().toString();
		this.flowName = null;
		this.process = null;
		this.processPath = null;
		this.elements = emptyList();
		this.findType = null;
		this.startElement = null;
		this.tasks = emptyList();
		this.totalDuration = Duration.ZERO;
		this.executionTime = 0;
		this.alternativeFlows = new HashMap<>();
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

	public String getProcessPath() {
		return processPath;
	}

	public void setProcessPath(String processPath) {
		this.processPath = processPath;
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

	public UseCase getUseCase() {
		return useCase;
	}

	public void setUseCase(UseCase useCase) {
		this.useCase = useCase;
	}

	public List<EstimatedElement> getTasks() {
		return tasks;
	}

	public void setTasks(List<EstimatedElement> tasks) {
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

	public String getDisplayTotalDuration() {
		return DateTimeHelper.getDisplayDuration (totalDuration);
	}

	public String getElementNames() {
		return this.tasks.stream().map(EstimatedElement::getElementName).collect(Collectors.joining(" -> "));
	}

	public String getTaskNames() {
		return this.tasks.stream().map(EstimatedElement::getTaskName).collect(Collectors.joining(" -> "));
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public List<Alternative> getAlternatives() {
		return alternatives;
	}

	public void setAlternatives(List<Alternative> alternatives) {
		this.alternatives = alternatives;
	}

	public Map<Alternative, SequenceFlow> getAlternativeFlows() {
		return alternativeFlows;
	}

	public void setAlternativeFlows(Map<Alternative, SequenceFlow> alternativeFlows) {
		this.alternativeFlows = alternativeFlows;
	}
}

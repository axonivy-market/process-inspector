package com.axonivy.utils.process.analyzer.internal;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import com.axonivy.utils.process.analyzer.helper.DateTimeHelper;
import com.axonivy.utils.process.analyzer.helper.TaskHelper;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.model.DetectedElement;
import com.axonivy.utils.process.analyzer.model.DetectedTask;
import com.axonivy.utils.process.analyzer.model.ElementTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.ProcessKind;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.workflow.ICallStack;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.IProcessData;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.TaskState;

public class ProcessAnalyzer implements AdvancedProcessAnalyzer {
	private static final List<TaskState> OPEN_TASK_STATES = List.of(TaskState.SUSPENDED, TaskState.PARKED,
			TaskState.RESUMED);

	private boolean isEnableDescribeAlternative;
	private Map<ElementTask, Duration> durationOverrides = emptyMap();
	private Map<String, String> processFlowOverrides = emptyMap();

	@Override
	public void enableDescribeAlternativeElements() {
		this.isEnableDescribeAlternative = true;
	}

	@Override
	public void disableDescribeAlternativeElements() {
		this.isEnableDescribeAlternative = false;
	}

	@Override
	public List<DetectedElement> findAllTasks(BaseElement startAtElement, Enum<?> useCase) throws Exception {
		return findAllDetectedElements(List.of(startAtElement), useCase);
	}

	@Override
	public List<DetectedElement> findAllTasks(List<BaseElement> startAtElements, Enum<?> useCase) throws Exception {
		return findAllDetectedElements(startAtElements, useCase);
	}

	@Override
	public List<DetectedElement> findAllTasks(ICase icase, Enum<?> useCase) throws Exception {
		Map<ProcessElement, Duration> elementsWithSpentDuration = getStartElementsWithSpentDuration(icase);

		return findAllDetectedElements(elementsWithSpentDuration, useCase);
	}

	@Override
	public List<DetectedElement> findTasksOnPath(BaseElement startAtElement, Enum<?> useCase, String flowName)
			throws Exception {
		return findDetectedElementsOnPath(List.of(startAtElement), useCase, flowName);
	}

	@Override
	public List<DetectedElement> findTasksOnPath(List<BaseElement> startAtElements, Enum<?> useCase, String flowName)
			throws Exception {
		return findDetectedElementsOnPath(startAtElements, useCase, flowName);
	}

	@Override
	public List<DetectedElement> findTasksOnPath(ICase icase, Enum<?> useCase, String flowName) throws Exception {
		Map<ProcessElement, Duration> elementsWithSpentDuration = getStartElementsWithSpentDuration(icase);
		return findDetectedElementsOnPath(elementsWithSpentDuration, useCase, flowName);
	}

	@Override
	public Duration calculateWorstCaseDuration(BaseElement startElement, Enum<?> useCase) throws Exception {
		Duration total = calculateMaxDuration(List.of(startElement), useCase);
		return total;
	}

	@Override
	public Duration calculateWorstCaseDuration(List<BaseElement> startElements, Enum<?> useCase) throws Exception {
		Duration total = calculateMaxDuration(startElements, useCase);
		return total;
	}

	@Override
	public Duration calculateWorstCaseDuration(ICase icase, Enum<?> useCase) throws Exception {
		List<BaseElement> elements = getStartElements(icase);
		Duration total = calculateMaxDuration(elements, useCase);
		return total;
	}

	@Override
	public Duration calculateDurationOfPath(BaseElement startElement, Enum<?> useCase, String flowName)
			throws Exception {
		Duration total = calculateMaxDuration(List.of(startElement), useCase, flowName);
		return total;
	}

	@Override
	public Duration calculateDurationOfPath(List<BaseElement> startElements, Enum<?> useCase, String flowName)
			throws Exception {
		Duration total = calculateMaxDuration(startElements, useCase, flowName);
		return total;
	}

	@Override
	public Duration calculateDurationOfPath(ICase icase, Enum<?> useCase, String flowName) throws Exception {
		List<BaseElement> elements = getStartElements(icase);
		Duration total = calculateMaxDuration(elements, useCase, flowName);
		return total;
	}

	@Override
	public AdvancedProcessAnalyzer setProcessFlowOverrides(Map<String, String> processFlowOverrides) {
		this.processFlowOverrides = processFlowOverrides;
		return this;
	}

	@Override
	public AdvancedProcessAnalyzer setDurationOverrides(Map<ElementTask, Duration> durationOverrides) {
		this.durationOverrides = durationOverrides;
		return this;
	}

	private List<DetectedElement> findAllDetectedElements(List<BaseElement> startAtElements, Enum<?> useCase)
			throws Exception {

		Map<ProcessElement, Duration> startAtElementWithDurations = initTimeUntilStart(startAtElements);
		List<DetectedElement> detectedTasks = findAllDetectedElements(startAtElementWithDurations, useCase);
		return detectedTasks;
	}

	private List<DetectedElement> findAllDetectedElements(Map<ProcessElement, Duration> startAtElementWithDuration,
			Enum<?> useCase) throws Exception {
		
		List<DetectedElement> detectedTasks = this.workflowPath().findAllTasks(startAtElementWithDuration, useCase);

		return detectedTasks;
	}

	private List<DetectedElement> findDetectedElementsOnPath(List<BaseElement> startAtElements, Enum<?> useCase,
			String flowName) throws Exception {
		Map<ProcessElement, Duration> startAtDurations = initTimeUntilStart(startAtElements);
		return findDetectedElementsOnPath(startAtDurations, useCase, flowName);
	}

	private List<DetectedElement> findDetectedElementsOnPath(Map<ProcessElement, Duration> startAtElementWithDuration,
			Enum<?> useCase, String flowName) throws Exception {
		List<DetectedElement> detectedTasks = this.workflowPath().findTaskOnPath(startAtElementWithDuration, useCase,
				flowName);

		return detectedTasks;
	}

	private Duration calculateMaxDuration(List<BaseElement> startAtElements, Enum<?> useCase) throws Exception {
		return calculateMaxDuration(startAtElements, useCase, null, true);
	}

	private Duration calculateMaxDuration(List<BaseElement> startAtElements, Enum<?> useCase, String flowName)
			throws Exception {
		return calculateMaxDuration(startAtElements, useCase, flowName, false);
	}

	private Duration calculateMaxDuration(List<BaseElement> elements, Enum<?> useCase, String flowName,
			boolean isFindAllTask) throws Exception {

		Map<ProcessElement, Duration> startAtDurations = initTimeUntilStart(elements);
		List<DetectedElement> detectedTasks = isFindAllTask ? findAllDetectedElements(startAtDurations, useCase)
				: findDetectedElementsOnPath(startAtDurations, useCase, flowName);

		return detectedTasks.stream().filter(it -> it instanceof DetectedTask).map(DetectedTask.class::cast)
				.map(DetectedTask::getTimeUntilEnd).max(Duration::compareTo).orElse(Duration.ZERO);
	}

	private Map<ProcessElement, Duration> getStartElementsWithSpentDuration(ICase icase) {
		List<ITask> tasks = getCaseITasks(icase);
		Map<ProcessElement, Duration> result = new LinkedHashMap<>();
		for (ITask task : tasks) {
			BaseElement element = TaskHelper.getBaseElementOf(task);			
			if (isTaskInCallableSub(element)) {
				element = getOriginalCallerElement(task);
			}
			
			Duration spentDuration = DateTimeHelper.getBusinessDuration(task.getStartTimestamp(), new Date());

			// Around to minutes
			result.put(new CommonElement(element), Duration.ZERO.minus(spentDuration));
		}

		return result;
	}

	private List<BaseElement> getStartElements(ICase icase) {
		List<ITask> tasks = getCaseITasks(icase);
		List<BaseElement> elements = new ArrayList<>();
		for (ITask task : tasks) {
			BaseElement element = TaskHelper.getBaseElementOf(task);
			if (isTaskInCallableSub(element)) {
				element = getOriginalCallerElement(task);
			}			
			elements.add(element);
		}

		return elements;
	}

	private List<ITask> getCaseITasks(ICase icase) {
		List<ITask> tasks = icase.tasks().all().stream()
				.filter(task -> OPEN_TASK_STATES.contains(task.getState()))
				.filter(it -> it.getCase().uuid().equals(icase.uuid()))
				.toList();
				
		return tasks;
	}

	private Map<ProcessElement, Duration> initTimeUntilStart(List<BaseElement> elements) {
		Map<ProcessElement, Duration> timeUntilStartAts = new LinkedHashMap<>();
		elements.forEach(it -> {
			timeUntilStartAts.put(new CommonElement(it), Duration.ZERO);
		});
		return timeUntilStartAts;
	}

	private WorkflowPath workflowPath() {
		return new WorkflowPath().setDurationOverrides(durationOverrides).setProcessFlowOverrides(processFlowOverrides)
				.setIsEnableDescribeAlternative(isEnableDescribeAlternative);
	}
	
	private BaseElement getOriginalCallerElement(ITask task) {
		String[] returnAddressPaths = ofNullable(task.getInternalStartProcessData())
				.map(IProcessData::getCallStack)
				.map(ICallStack::getReturnAddressPath)
				.orElse(ArrayUtils.EMPTY_STRING_ARRAY);

		BaseElement originalElement = Stream.of(returnAddressPaths)
				.map(it -> getOriginalCallerId(it))
				.filter(Objects::nonNull).map(PID::of)
				.map(pid -> TaskHelper.getBaseElementByPid(pid, task.getProcessModelVersion()))
				.findFirst()
				.orElse(null);

		return originalElement;
	}
	
	private String getOriginalCallerId(String addressPath) {
		// Path example: 2:18DE58E0441486DF-S30-f1/jump=18EEB810DDACA1D0-f0/isExt=true/uuid=3642e994-4f46-4c41-96ad-0d9d368cb5a0
		String[] elementIds = addressPath.split("/");
		if (ArrayUtils.isNotEmpty(elementIds)) {
			// callNodeId: 2:18DE58E0441486DF-S30-f1
			String callerNodeId = elementIds[0];
			return callerNodeId.split(":")[1];
		}
		return null;
	}

	private boolean isTaskInCallableSub(BaseElement element) {
		return ProcessKind.CALLABLE_SUB.equals(element.getRootProcess().getKind());
	}
}

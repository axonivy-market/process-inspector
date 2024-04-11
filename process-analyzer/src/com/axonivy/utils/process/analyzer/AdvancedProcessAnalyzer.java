package com.axonivy.utils.process.analyzer;

import static java.util.Collections.emptyMap;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.model.DetectedElement;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;

@SuppressWarnings("restriction")
public class AdvancedProcessAnalyzer extends ProcessAnalyzer {

	private Process process;
	private Enum<?> useCase;
	private String flowName;
	private boolean isEnableDescribeAlternative;
	private Map<String, Duration> durationOverrides;
	// It only impart to find task base in flowName
	private Map<String, String> processFlowOverrides;
	
	/** 
	 * @param process - The process that should be analyzed.
	 * @param useCase - Use case that should be used to read duration values.
	 * If it is null, it will get first duration configure line
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 */
	public AdvancedProcessAnalyzer(Process process, Enum<?> useCase, String flowName) {
		super();
		this.process = process;
		this.useCase = useCase;
		this.flowName = flowName;
		this.isEnableDescribeAlternative = false;
		this.durationOverrides = emptyMap();
		this.processFlowOverrides = emptyMap();		
	}
	
	@Override
	protected Map<String, Duration> getDurationOverrides() {		
		return durationOverrides;
	}

	@Override
	protected Map<String, String> getProcessFlowOverrides() {
		return processFlowOverrides;
	}
	
	/**
	 * Disabled by default.
	 * If this option is enabled, the Advanced Process Analyzer will also add all alternative elements to the result.
	 * This option will affect findTasksOnPath as well as findAllTasks method. Both methods will traverse the process as usual.
	 * When it bypasses an alternative element, it will be added to the result list.
	 */
	public void enableDescribeAlternativeElements() {
		this.isEnableDescribeAlternative = true;
	}

	public void disableDescribeAlternativeElements() {
		this.isEnableDescribeAlternative = false;	
	}
	
	@Override
	protected boolean isDescribeAlternativeElements() {
		return isEnableDescribeAlternative;
	}

	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElement - Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findAllTasks(BaseElement startAtElement) throws Exception {
		Map<ProcessElement, List<ProcessElement>> path = findPath(new CommonElement(startAtElement));
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase);
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findAllTasks(List<BaseElement> startAtElements) throws Exception {
		CommonElement[] elements = startAtElements.stream().map(CommonElement::new).toArray(CommonElement[]::new);
		Map<ProcessElement, List<ProcessElement>> path = findPath(elements);
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase);
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks in the process which can be reached from active case.
	 * @param icase - Ivy Case which should be analyzed.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findAllTasks(ICase icase) throws Exception {		
		List<ITask> tasks = getCaseITasks(icase);
		Map<ProcessElement, Date> elementsWithTime = getProcessElementWithStartTimestamp(tasks);
		ProcessElement[] elements = elementsWithTime.keySet().stream().toArray(CommonElement[]::new);
		
		Map<ProcessElement, List<ProcessElement>> path = findPath(elements);		
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, elementsWithTime);
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param startAtElement - Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findTasksOnPath(BaseElement startAtElement) throws Exception {
		ProcessElement element = new CommonElement(startAtElement);
		Map<ProcessElement, List<ProcessElement>> path = findPath(flowName, element);
		
		Map<ProcessElement, Date> startedAts = Map.of(element, new Date());		
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, startedAts);
		return detectedTasks;
	}
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findTasksOnPath(List<BaseElement> startAtElements) throws Exception {
		ProcessElement[] elements = startAtElements.stream().map(CommonElement::new).toArray(CommonElement[]::new);
		Map<ProcessElement, List<ProcessElement>> path = findPath(flowName, elements);
		
		Map<ProcessElement, Date> startedAts = Stream.of(elements).collect(Collectors.toMap(it ->it, it -> new Date()));
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, startedAts);
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param icase - Ivy Case which should be analyzed.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findTasksOnPath(ICase icase) throws Exception {		
		List<ITask> tasks = getCaseITasks(icase);
		Map<ProcessElement, Date> elementsWithTime = getProcessElementWithStartTimestamp(tasks);
		ProcessElement[] elements = elementsWithTime.keySet().stream().toArray(CommonElement[]::new);		
		Map<ProcessElement, List<ProcessElement>> path = findPath(flowName, elements);
		
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, elementsWithTime);
		return detectedTasks;
	}
	
	/**
	 * This method can be used to calculate expected duration from a starting point in a process until all task are done and end of process is reached.
	 * It will summarize duration of all tasks on the path. In case of parallel process flows, it will always use the critical path (which means path with longer duration).
	 * @param startElement - Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(BaseElement startElement) throws Exception {
		ProcessElement element = new CommonElement(startElement);
		Map<ProcessElement, List<ProcessElement>> path = flowName != null ? findPath(flowName, element) : findPath(element);
		
		Duration total = calculateTotalDuration(path, useCase);
		
		return total;
	}
	
	/** 
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param startElements
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(List<BaseElement> startElements) throws Exception {
		ProcessElement[] elements = startElements.stream().map(CommonElement::new).toArray(CommonElement[]::new);
		Map<ProcessElement, List<ProcessElement>> path = flowName != null ? findPath(flowName, elements) : findPath(elements);
		
		//We only get max total duration on each path
		Duration total = calculateTotalDuration(path, useCase);
		
		return total;
	}

	/** 
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param icase - Ivy Case which should be analyzed.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(ICase icase) throws Exception {
		List<ITask> tasks = getCaseITasks(icase);
		Map<ProcessElement, Date> elementsWithTime = getProcessElementWithStartTimestamp(tasks);
		ProcessElement[] elements = elementsWithTime.keySet().stream().toArray(CommonElement[]::new);
		
		Map<ProcessElement, List<ProcessElement>> path = flowName != null ? findPath(flowName, elements) : findPath(elements);		
		Duration total = calculateTotalDuration(path, useCase);
		
		return total;
	}
	
	/**
	 * This method can be used to override configured path taken after an alternative gateway.
	 * @param processFlowOverrides
	 * key: element ID + task identifier (for support of callable sub-processes, we also need to add the path of parent elements. However, not needed in first versions.)
	 * value: chosen output PID
	 * @return
	 */
	public AdvancedProcessAnalyzer setProcessFlowOverrides(HashMap<String, String> processFlowOverrides) {
		this.processFlowOverrides = processFlowOverrides;
		return this;
	}
	
	/**
	 * This method can be used to override configured task duration of the model by own values.
	 * @param durationOverrides
	 * key: element ID + task identifier (for support of callable sub-processes, we also need to add the path of parent elements. However, not needed in first versions.)
	 * value: new duration
	 * @return
	 */
	public AdvancedProcessAnalyzer setDurationOverrides(HashMap<String, Duration> durationOverrides) {
		this.durationOverrides = durationOverrides;
		return this;
	}
}
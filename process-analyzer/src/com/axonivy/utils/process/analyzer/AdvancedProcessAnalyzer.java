package com.axonivy.utils.process.analyzer;

import static java.util.Collections.emptyMap;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;
import com.axonivy.utils.process.analyzer.internal.model.AnalysisPath;
import com.axonivy.utils.process.analyzer.internal.model.CommonElement;
import com.axonivy.utils.process.analyzer.internal.model.ProcessElement;
import com.axonivy.utils.process.analyzer.model.DetectedElement;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.workflow.ICase;

public class AdvancedProcessAnalyzer extends ProcessAnalyzer {

	private Process process;
	private boolean isEnableDescribeAlternative;
	private Map<String, Duration> durationOverrides;
	// It only impart to find task base in flowName
	private Map<String, String> processFlowOverrides;
	
	/** 
	 * @param process - The process that should be analyzed.
	 * If it is null, it will get first duration configure line
	 */
	public AdvancedProcessAnalyzer(Process process) {
		super();
		this.process = process;
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
	
	@Override
	protected boolean isDescribeAlternativeElements() {
		return isEnableDescribeAlternative;
	}

	/** 
	 * If this option is enabled, the Advanced Process Analyzer will also add all alternative elements to the result.
	 * This option will affect findTasksOnPath as well as findAllTasks method. Disabled by default.
	 * When it bypasses an alternative element, it will be added to the result list. 
	 */
	public void enableDescribeAlternativeElements() {
		this.isEnableDescribeAlternative = true;
	}

	/**
	 * When it bypasses an alternative element, it will be not added to the result list.
	 */
	public void disableDescribeAlternativeElements() {
		this.isEnableDescribeAlternative = false;	
	}
	

	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findAllTasks(BaseElement startAtElement, Enum<?> useCase) throws Exception {
		List<ProcessElement> elements =  List.of(new CommonElement(startAtElement));
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements);
		
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, initTimeUntilStart(elements));
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findAllTasks(List<BaseElement> startAtElements, Enum<?> useCase) throws Exception {
		List<ProcessElement> elements = convertToProcessElements(startAtElements);
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements);
		
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, initTimeUntilStart(elements));
		return detectedTasks;
	}

	/**
	 * Return a list of all tasks in the process which can be reached from active case.
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findAllTasks(ICase icase, Enum<?> useCase) throws Exception {			
		List<ProcessElement> elements = getStartElements(icase);		
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements);
		
		Map<ProcessElement, Duration> elementsWithSpentDuration = getStartElementsWithSpentDuration(icase);
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, elementsWithSpentDuration);
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param startAtElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @param flowName - Tag name we want to follow at alternative gateways. 	 
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findTasksOnPath(BaseElement startAtElement, Enum<?> useCase, String flowName) throws Exception {
		List<ProcessElement> elements = List.of(new CommonElement(startAtElement));		
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements, flowName);
			
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, initTimeUntilStart(elements));
		return detectedTasks;
	}
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findTasksOnPath(List<BaseElement> startAtElements, Enum<?> useCase, String flowName) throws Exception {
		List<ProcessElement> elements = convertToProcessElements(startAtElements);
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements, flowName);
		
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, initTimeUntilStart(elements));
		return detectedTasks;
	}
	
	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 * @return
	 * @throws Exception
	 */
	public List<? extends DetectedElement> findTasksOnPath(ICase icase, Enum<?> useCase, String flowName) throws Exception {
		List<DetectedElement> detectedTasks = findTasksByCase(icase, useCase, flowName, false);
		return detectedTasks;
	}
	
	private List<DetectedElement> findTasksByCase(ICase icase, Enum<?> useCase, String flowName, boolean isFindAllTasks) throws Exception {
		List<ProcessElement> elements = getStartElements(icase);
		Map<ProcessElement, List<AnalysisPath>> path = isFindAllTasks? findPath(elements): findPath(elements, flowName);
		
		Map<ProcessElement, Duration> elementsWithSpentDuration = getStartElementsWithSpentDuration(icase);
		List<DetectedElement> detectedTasks = convertToDetectedElements(path, useCase, elementsWithSpentDuration);
		return detectedTasks;
	}
	
	/**
	 * This method can be used to calculate expected worst case duration from a starting point in a process until all task are done and end of process is reached.
	 * In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateWorstCaseDuration(BaseElement startElement, Enum<?> useCase) throws Exception {
		ProcessElement element = new CommonElement(startElement);		
		Map<ProcessElement, List<AnalysisPath>> path = findPath(List.of(element));
		
		Duration total = calculateTotalDuration(path, useCase);		
		return total;
	}
	
	/** 
	 * This method can be used to calculate expected worst case duration from a starting point in a process until all task are done and end of process is reached.
	 * In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateWorstCaseDuration(List<BaseElement> startElements, Enum<?> useCase) throws Exception {
		List<ProcessElement> elements = convertToProcessElements(startElements);		
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements);
		
		Duration total = calculateTotalDuration(path, useCase);		
		return total;
	}

	/** 
	 * This method can be used to calculate expected worst case duration from a starting point in a process until all task are done and end of process is reached.
	 * In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateWorstCaseDuration(ICase icase, Enum<?> useCase) throws Exception {
		List<ProcessElement> elements = getStartElements(icase);
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements);
				
		Duration total = calculateTotalDuration(path, useCase);		
		return total;
	}
	
	/**
	 * This method can be used to calculate expected duration from a starting point using a named flow or default flow. 
	 * For parallel segments of the process, it will still use the “critical path” (same logic like worst case duration).	 * 
	 * @param startElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateDurationOfPath(BaseElement startElement, Enum<?> useCase, String flowName) throws Exception {
		ProcessElement element = new CommonElement(startElement);		
		Map<ProcessElement, List<AnalysisPath>> path = findPath(List.of(element), flowName);
		
		Duration total = calculateTotalDuration(path, useCase);		
		return total;
	}
	
	/** 
	 * This method can be used to calculate expected duration from a starting point using a named flow or default flow. 
	 * For parallel segments of the process, it will still use the “critical path” (same logic like worst case duration). 
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateDurationOfPath(List<BaseElement> startElements, Enum<?> useCase, String flowName) throws Exception {
		List<ProcessElement> elements = convertToProcessElements(startElements);	
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements, flowName);
		
		Duration total = calculateTotalDuration(path, useCase);		
		return total;
	}

	/** 
	 * This method can be used to calculate expected duration from a starting point using a named flow or default flow. 
	 * For parallel segments of the process, it will still use the “critical path” (same logic like worst case duration).
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateDurationOfPath(ICase icase, Enum<?> useCase, String flowName) throws Exception {
		List<ProcessElement> elements = getStartElements(icase);
		Map<ProcessElement, List<AnalysisPath>> path = findPath(elements, flowName);
				
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
	
	private List<ProcessElement> convertToProcessElements(List<BaseElement> elements) {
		return elements.stream().map(CommonElement::new).map(ProcessElement.class::cast).toList();
	}
	
	private Map<ProcessElement, Duration> initTimeUntilStart(List<ProcessElement> elements) {
		Map<ProcessElement, Duration> timeUntilStartAts = elements.stream().collect(Collectors.toMap(it -> it, it -> Duration.ZERO));
		return timeUntilStartAts;
	}
}
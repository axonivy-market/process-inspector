package com.axonivy.utils.process.analyzer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.process.analyzer.model.DetectedElement;
import com.axonivy.utils.process.analyzer.model.ElementTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.workflow.ICase;

/**
 *  The AdvancedProcessAnalyzer tool is used to calculate duration to finish a workflow case.
 *  It will list all tasks which must finish with duration until end base on estimate duration.
 *   
 *  The process diagram below with estimate duration for each task. 
 *  start -> Task A (1h) -> [alternative] -{internal}-> Task B (2h) -> EndTask
 *  							    	 |------------> Task C (3h) -> EndTask
 * How to find all tasks ? 
 * 	AdvancedProcessAnalyzer processAnalyzer = new ProcessAnalyzer();
 * 	List<DetectedElement> result = processAnalyzer.findAllTasks(start, UseCase.BIGPROJECT);
 * 	Result: Task A (1h), Task B (3h), Task C (4h)
 *  => So duration to finish all tasks will task 4hours
 * 
 * How to find tasks on path? 
 *  AdvancedProcessAnalyzer processAnalyzer = new ProcessAnalyzer();
 * 	List<DetectedElement> result = processAnalyzer.findTasksOnPath(start, UseCase.BIGPROJECT, "internal");
 * 	Result: Task A (1h), Task B (3h)
 *  So duration to finish all tasks will task 3hours
 */

public interface AdvancedProcessAnalyzer {

	/** 
	 * If this option is enabled, the Advanced Process Analyzer will also add all alternative elements to the result.
	 * This option will affect findTasksOnPath as well as findAllTasks method. Disabled by default.
	 * When it bypasses an alternative element, it will be added to the result list. 
	 */
	public void enableDescribeAlternativeElements();

	/**
	 * When it bypasses an alternative element, it will be not added to the result list.
	 */
	public void disableDescribeAlternativeElements();

	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public List<DetectedElement> findAllTasks(BaseElement startAtElement, Enum<?> useCase) throws Exception;
	
	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public List<DetectedElement> findAllTasks(List<BaseElement> startAtElements, Enum<?> useCase) throws Exception;

	/**
	 * Return a list of all tasks in the process which can be reached from active case.
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public List<DetectedElement> findAllTasks(ICase icase, Enum<?> useCase) throws Exception;
	
	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param startAtElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @param flowName - Tag name we want to follow at alternative gateways. 	 
	 * @return
	 * @throws Exception
	 */
	public List<DetectedElement> findTasksOnPath(BaseElement startAtElement, Enum<?> useCase, String flowName) throws Exception;
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 * @return
	 * @throws Exception
	 */
	public List<DetectedElement> findTasksOnPath(List<BaseElement> startAtElements, Enum<?> useCase, String flowName) throws Exception;
	
	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 * @return
	 * @throws Exception
	 */
	public List<DetectedElement> findTasksOnPath(ICase icase, Enum<?> useCase, String flowName) throws Exception;	
	
	/**
	 * This method can be used to calculate expected worst case duration from a starting point in a process until all task are done and end of process is reached.
	 * In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateWorstCaseDuration(BaseElement startElement, Enum<?> useCase) throws Exception;
	
	/** 
	 * This method can be used to calculate expected worst case duration from a starting point in a process until all task are done and end of process is reached.
	 * In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateWorstCaseDuration(List<BaseElement> startElements, Enum<?> useCase) throws Exception;

	/** 
	 * This method can be used to calculate expected worst case duration from a starting point in a process until all task are done and end of process is reached.
	 * In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateWorstCaseDuration(ICase icase, Enum<?> useCase) throws Exception;
	
	/**
	 * This method can be used to calculate expected duration from a starting point using a named flow or default flow. 
	 * For parallel segments of the process, it will still use the “critical path” (same logic like worst case duration).	 * 
	 * @param startElement - Element where we start traversing the process
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateDurationOfPath(BaseElement startElement, Enum<?> useCase, String flowName) throws Exception;
	
	/** 
	 * This method can be used to calculate expected duration from a starting point using a named flow or default flow. 
	 * For parallel segments of the process, it will still use the “critical path” (same logic like worst case duration). 
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateDurationOfPath(List<BaseElement> startElements, Enum<?> useCase, String flowName) throws Exception;

	/** 
	 * This method can be used to calculate expected duration from a starting point using a named flow or default flow. 
	 * For parallel segments of the process, it will still use the “critical path” (same logic like worst case duration).
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param icase - Ivy Case which should be analyzed.
	 * @param useCase - Use case that should be used to read duration values. Durations will be set to 0 in case not provided.
	 * @return
	 * @throws Exception
	 */
	public Duration calculateDurationOfPath(ICase icase, Enum<?> useCase, String flowName) throws Exception;
	
	/**
	 * This method can be used to override configured path taken after an alternative gateway.
	 * @param processFlowOverrides
	 * key: alternative element PID
	 * value: chosen output PID
	 * @return
	 */
	public AdvancedProcessAnalyzer setProcessFlowOverrides(Map<String, String> processFlowOverrides);
	
	/**
	 * This method can be used to override configured task duration of the model by own values.
	 * @param durationOverrides
	 * key: ElementTask -  with ElementTask.createGateway(element ID, task identifier) for TaskSwitchGateway, ElementTask.createSingle(element ID) for other  
	 * value: new duration
	 * @return
	 */
	public AdvancedProcessAnalyzer setDurationOverrides(Map<ElementTask, Duration> durationOverrides);
}
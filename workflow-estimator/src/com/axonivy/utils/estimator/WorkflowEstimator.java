package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public class WorkflowEstimator {

	private Enum<?> useCase;
	private String flowName;
	
	public final ProcessGraph graph;
	
	/** 
	 * @param process - The process that should be analyzed.
	 * @param useCase - Use case that should be used to read duration values.
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 */
	public WorkflowEstimator(Process process, Enum<?> useCase, String flowName) {		
		this.useCase = useCase;
		this.flowName = flowName;
		this.graph = new ProcessGraph(process);
	}

	/**
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElement – Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public List<EstimatedTask> findAllTasks(BaseElement startAtElement) throws Exception {
		List<BaseElement> path = graph.findPath(startAtElement);
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @return
	 * @throws Exception
	 */
	public List<EstimatedTask> findAllTasks(List<BaseElement> startAtElements) throws Exception {
		List<BaseElement> path = graph.findPath(startAtElements.toArray(new BaseElement[0]));
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}

	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param startAtElement – Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) throws Exception {
		List<BaseElement> path = graph.findPath(flowName, startAtElement);
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @return
	 * @throws Exception
	 */
	public List<EstimatedTask> findTasksOnPath(List<BaseElement> startAtElements) throws Exception {
		List<BaseElement> path = graph.findPath(flowName, startAtElements.toArray(new BaseElement[0]));
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}
	
	/**
	 * This method can be used to calculate expected duration from a starting point in a process until all task are done and end of process is reached.
	 * It will summarize duration of all tasks on the path. In case of parallel process flows, it will always use the “critical path” (which means path with longer duration).
	 * @param startElement – Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(BaseElement startElement) throws Exception {
		List<BaseElement> path = isNotEmpty(flowName) 
				? graph.findPath(flowName, startElement)
				: graph.findPath(startElement);
		
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		
		Duration total = estimatedTasks.stream().map(EstimatedTask::getEstimatedDuration).reduce((a,b) -> a.plus(b)).orElse(Duration.ZERO);
		
		return total;
	}
	
	/** 
	 * @param startElement – Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param startElements
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(List<BaseElement> startElements) throws Exception {
		List<BaseElement> path = isNotEmpty(flowName) 
				? graph.findPath(flowName, startElements.toArray(new BaseElement[0]))
				: graph.findPath(startElements.toArray(new BaseElement[0]));
		
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		
		Duration total = estimatedTasks.stream().map(EstimatedTask::getEstimatedDuration).reduce((a,b) -> a.plus(b)).orElse(Duration.ZERO);
		
		return total;
	}

	/**
	 * This method can be used to override configured path taken after an alternative gateway.
	 * @param processFlowOverrides
	 * key: element ID + task identifier (for support of callable sub-processes, we also need to add the path of parent elements. However, not needed in first versions.)
	 * value: chosen output PID
	 * @return
	 */
	public WorkflowEstimator setProcessFlowOverrides(HashMap<String, String> processFlowOverrides) {
		graph.setProcessFlowOverrides(processFlowOverrides);
		return this;
	}
	
	/**
	 * This method can be used to override configured task duration of the model by own values.
	 * @param durationOverrides
	 * key: element ID + task identifier (for support of callable sub-processes, we also need to add the path of parent elements. However, not needed in first versions.)
	 * value: new duration
	 * @return
	 */
	public WorkflowEstimator setDurationOverrides(HashMap<String, Duration> durationOverrides) {
		graph.setDurationOverrides(durationOverrides);
		return this;
	}
	
	private List<EstimatedTask> convertToEstimatedTasks(List<BaseElement> path) {

		List<TaskAndCaseModifier> taskPath = filterAcceptedTask(path);

		// Convert to EstimatedTask
		List<EstimatedTask> estimatedTasks = new ArrayList<>();
		for (int i = 0; i < taskPath.size(); i++) {
			List<EstimatedTask> estimatedTaskResults = new ArrayList<>();
			Date startTimestamp = i == 0 ? new Date() : estimatedTasks.get(estimatedTasks.size() - 1).calculateEstimatedEndTimestamp();
			estimatedTaskResults = createEstimatedTask(taskPath.get(i), startTimestamp);

			estimatedTasks.addAll(estimatedTaskResults);
		}
		return estimatedTasks;
	}
	
	private List<TaskAndCaseModifier> filterAcceptedTask(List<BaseElement> path) {
		return path.stream()
				// filter to get task only
				.filter(node -> {
					return node instanceof TaskAndCaseModifier;
				}).map(TaskAndCaseModifier.class::cast)
				.filter(node -> {
					return node instanceof RequestStart == false;
				})
				// Remove SYSTEM task
				.filter(node -> {
					return ProcessGraph.isSystemTask(node) == false;
				}).map(TaskAndCaseModifier.class::cast)
				// filter the task which have estimated if needed
				.toList();
	}
	
	private List<EstimatedTask> createEstimatedTask(TaskAndCaseModifier task, Date startTimestamp) {
		List<TaskConfig> taskConfigs = task.getAllTaskConfigs();
		
		List<EstimatedTask> estimatedTasks = new ArrayList<>();
				
		taskConfigs.forEach(taskConfig -> {
			EstimatedTask estimatedTask = new EstimatedTask();
			
			estimatedTask.setPid(graph.getTaskId(task, taskConfig));		
			estimatedTask.setParentElementNames(graph.getParentElementNames(task));
			estimatedTask.setTaskName(taskConfig.getName().getRawMacro());
			estimatedTask.setElementName(task.getName());
			Duration estimatedDuration = graph.getDuration(task, taskConfig);				
			estimatedTask.setEstimatedDuration(estimatedDuration);
			estimatedTask.setEstimatedStartTimestamp(startTimestamp);		
			String customerInfo = graph.getCustomInfoByCode(taskConfig);
			estimatedTask.setCustomInfo(customerInfo);
			estimatedTasks.add(estimatedTask);
		});
		
		return estimatedTasks.stream()
				.sorted(Comparator.comparing(EstimatedTask::getTaskName))
				.toList();
	}
}
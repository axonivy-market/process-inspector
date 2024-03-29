package com.axonivy.utils.estimator;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.internal.AbstractWorkflow;
import com.axonivy.utils.estimator.model.EstimatedElement;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public class WorkflowEstimator extends AbstractWorkflow {

	private Process process;
	private UseCase useCase;
	private String flowName;
	private Map<String, Duration> durationOverrides;
	// It only impart to find task base in flowName
	private Map<String, String> processFlowOverrides;
	
	/** 
	 * @param process - The process that should be analyzed.
	 * @param useCase - Use case that should be used to read duration values.
	 * If it is null, it will get first duration configure line
	 * @param flowName - Tag name we want to follow at alternative gateways.
	 */
	public WorkflowEstimator(Process process, UseCase useCase, String flowName) {
		super();
		this.process = process;
		this.useCase = useCase;
		this.flowName = flowName;
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
	 * Return a list of all tasks in the process which can be reached from the starting element.
	 * @param startAtElement - Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public List<? extends EstimatedElement> findAllTasks(BaseElement startAtElement) throws Exception {
		List<BaseElement> path = findPath(startAtElement);
		List<EstimatedElement> estimatedTasks = convertToEstimatedElements(path, useCase);
		return estimatedTasks;
	}
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @return
	 * @throws Exception
	 */
	public List<? extends EstimatedElement> findAllTasks(List<BaseElement> startAtElements) throws Exception {
		List<BaseElement> path = findPath(startAtElements.toArray(new BaseElement[0]));
		List<EstimatedElement> estimatedTasks = convertToEstimatedElements(path, useCase);
		return estimatedTasks;
	}

	/**
	 * Return a list of all tasks which are created when process follows the tagged flow. Uses the flow name set in the constructor.
	 * @param startAtElement - Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public List<? extends EstimatedElement> findTasksOnPath(BaseElement startAtElement) throws Exception {
		List<BaseElement> path = findPath(flowName, startAtElement);
		List<EstimatedElement> estimatedTasks = convertToEstimatedElements(path, useCase);
		return estimatedTasks;
	}
	
	/**
	 * @param startAtElements - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects.
	 * @return
	 * @throws Exception
	 */
	public List<? extends EstimatedElement> findTasksOnPath(List<BaseElement> startAtElements) throws Exception {
		List<BaseElement> path = findPath(flowName, startAtElements.toArray(new BaseElement[0]));
		List<EstimatedElement> estimatedTasks = convertToEstimatedElements(path, useCase);
		return estimatedTasks;
	}
	
	/**
	 * This method can be used to calculate expected duration from a starting point in a process until all task are done and end of process is reached.
	 * It will summarize duration of all tasks on the path. In case of parallel process flows, it will always use the critical path (which means path with longer duration).
	 * @param startElement - Element where we start traversing the process
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(BaseElement startElement) throws Exception {
		List<BaseElement> path = isNotEmpty(flowName) ? findPath(flowName, startElement) : findPath(startElement);
		
		List<EstimatedElement> estimatedTasks = convertToEstimatedElements(path, useCase);
		
		Duration total = estimatedTasks.stream()
				.filter(node -> node instanceof EstimatedTask)
				.map(EstimatedTask.class::cast)
				.map(EstimatedTask::getEstimatedDuration)
				.reduce((a,b) -> a.plus(b)).orElse(Duration.ZERO);
		
		return total;
	}
	
	/** 
	 * @param startElement - Elements where we start traversing the process. In case of parallel tasks, the list will contain multiple objects. 
	 * @param startElements
	 * @return
	 * @throws Exception
	 */
	public Duration calculateEstimatedDuration(List<BaseElement> startElements) throws Exception {
		BaseElement[] elements = startElements.toArray(new BaseElement[0]);
		List<BaseElement> path = isNotEmpty(flowName) ? findPath(flowName, elements) : findPath(elements);
		
		List<EstimatedElement> estimatedTasks = convertToEstimatedElements(path, useCase);
		
		Duration total = estimatedTasks.stream()
				.filter(node -> node instanceof EstimatedTask)
				.map(EstimatedTask.class::cast)
				.map(EstimatedTask::getEstimatedDuration)
				.reduce((a,b) -> a.plus(b)).orElse(Duration.ZERO);
		
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
	public WorkflowEstimator setDurationOverrides(HashMap<String, Duration> durationOverrides) {
		this.durationOverrides = durationOverrides;
		return this;
	}
	
	private List<EstimatedElement> convertToEstimatedElements(List<BaseElement> path, UseCase useCase) {	
		List<ProcessElement> taskPath = filterAcceptedTask(path);
		// convert to Estimated Task 
		List<EstimatedElement> result = new ArrayList<>();
		for (int i = 0; i < taskPath.size(); i++) {	
			Date startTimestamp = getEstimatedEndTimestamp(result);
			List<EstimatedTask> estimatedTaskResults = createEstimatedTask(taskPath.get(i), startTimestamp, useCase);
			result.addAll(estimatedTaskResults);
		}
		return result.stream().filter(item -> item != null).toList();		
	}
	
	private Date getEstimatedEndTimestamp(List<EstimatedElement> estimatedElements) {
		List<EstimatedTask> estimatedTasks = estimatedElements.stream()
				.filter(item -> item instanceof EstimatedTask)
				.map(EstimatedTask.class::cast)
				.toList();
		int size =  estimatedTasks.size();
		return size > 0 ? estimatedTasks.get(size - 1).calculateEstimatedEndTimestamp() : new Date();
	}

	private List<ProcessElement> filterAcceptedTask(List<BaseElement> path) {
		return path.stream()
				// filter to get accepted task 
				.filter(node -> isAcceptedElement(node))
				.map(ProcessElement.class::cast)
				.toList();
	}
	
	private boolean isAcceptedElement(BaseElement element) {
		if(element instanceof RequestStart) {
			return false;
		}
		if(element instanceof TaskAndCaseModifier) {
			return !isSystemTask((TaskAndCaseModifier)element);
		}
		if(element instanceof TaskAndCaseModifier || element instanceof Alternative) {
			return true;
		}
		return false;
	}
	
	private List<EstimatedTask> createEstimatedTask(ProcessElement element, Date startTimestamp, UseCase useCase) {	
		if(element instanceof TaskAndCaseModifier) {
			TaskAndCaseModifier task = (TaskAndCaseModifier) element;
			
			List<TaskConfig> taskConfigs = task.getAllTaskConfigs();
			
			List<EstimatedTask> estimatedTasks = new ArrayList<>();
					
			taskConfigs.forEach(taskConfig -> {
				EstimatedTask estimatedTask = new EstimatedTask();
				
				estimatedTask.setPid(getTaskId(task, taskConfig));		
				estimatedTask.setParentElementNames(getParentElementNames(task));
				estimatedTask.setTaskName(taskConfig.getName().getRawMacro());
				estimatedTask.setElementName(task.getName());
				Duration estimatedDuration = getDuration(task, taskConfig, useCase);				
				estimatedTask.setEstimatedDuration(estimatedDuration);
				estimatedTask.setEstimatedStartTimestamp(startTimestamp);		
				String customerInfo = getCustomInfoByCode(taskConfig);
				estimatedTask.setCustomInfo(customerInfo);
				estimatedTasks.add(estimatedTask);
			});
			
			return estimatedTasks.stream()
					.sorted(Comparator.comparing(EstimatedTask::getTaskName))
					.toList();
		}
		
		return Collections.emptyList();	
	}
}
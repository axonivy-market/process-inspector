package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;


@SuppressWarnings("restriction")
public class WorkflowEstimator {

	private Process process;
	private Enum<?> useCase;
	private String flowName;
	
	public final ProcessGraph graph;
	
	public WorkflowEstimator(Process process, Enum<?> useCase, String flowName) {
		this.process = process;
		this.useCase = useCase;
		this.flowName = flowName;
		this.graph = new ProcessGraph(process);
	}

	public List<EstimatedTask> findAllTasks(BaseElement startAtElement) throws Exception {
		List<BaseElement> path = graph.findPath(startAtElement);
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}
	
	public List<EstimatedTask> findAllTasks(List<BaseElement> startAtElements) throws Exception {
		List<BaseElement> path = graph.findPath(startAtElements.toArray(new BaseElement[0]));
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}

	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) throws Exception {
		List<BaseElement> path = graph.findPath(flowName, startAtElement);
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}
	
	public List<EstimatedTask> findTasksOnPath(List<BaseElement> startAtElements) throws Exception {
		List<BaseElement> path = graph.findPath(flowName, startAtElements.toArray(new BaseElement[0]));
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		return estimatedTasks;
	}
	
	public Duration calculateEstimatedDuration(BaseElement startElement) throws Exception {
		List<BaseElement> path = isNotEmpty(flowName) 
				? graph.findPath(flowName, startElement)
				: graph.findPath(startElement);
		
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(path);
		
		Duration total = estimatedTasks.stream().map(EstimatedTask::getEstimatedDuration).reduce((a,b) -> a.plus(b)).orElse(Duration.ZERO);
		
		return total;
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
					return isSystemTask(node) == false;
				}).map(TaskAndCaseModifier.class::cast)
				// filter the task which have estimated if needed
				.toList();
	}
	
	private List<EstimatedTask> createEstimatedTask(TaskAndCaseModifier task, Date startTimestamp) {
		List<TaskConfig> taskConfigs = task.getAllTaskConfigs();
		
		List<EstimatedTask> estimatedTasks = new ArrayList<>();
		
		taskConfigs.forEach(item -> {
			EstimatedTask estimatedTask = new EstimatedTask();
			
			estimatedTask.setPid(task.getPid().getRawPid());		
			estimatedTask.setParentElementNames(emptyList());
			estimatedTask.setTaskName(defaultIfEmpty(item.getName().getRawMacro(), task.getName()));
			Duration estimatedDuration = getDurationByCode(item);				
			estimatedTask.setEstimatedDuration(estimatedDuration);
			estimatedTask.setEstimatedStartTimestamp(startTimestamp);		
			String customerInfo = getCustomInfoByCode(item);
			estimatedTask.setCustomInfo(customerInfo);
			estimatedTasks.add(estimatedTask);
		});
		
		return estimatedTasks.stream()
				.sorted(Comparator.comparing(EstimatedTask::getTaskName))
				.toList();
	}

	private String getWfEstimateLineFromCode(TaskConfig task, String prefix) {
		// strongly typed!
		String script = Optional.of(task.getScript()).orElse(EMPTY);
		String[] codeLines = script.split("\\n");
		String wfEstimateCode = Arrays.stream(codeLines)
				.filter(line -> line.contains(prefix))
				.findFirst()
				.orElse(EMPTY);
		return wfEstimateCode;
	}
	
	private Duration getDurationByCode(TaskConfig task) {
		
		String wfEstimateCode = getWfEstimateLineFromCode(task, "WfEstimate.setEstimate");
		if (StringUtils.isNotEmpty(wfEstimateCode)) {
			String result = StringUtils.substringBetween(wfEstimateCode, "(", "UseCase");
			int amount = Integer.parseInt(result.substring(0, result.indexOf(",")));
			String unit = result.substring(result.indexOf(".") + 1, result.lastIndexOf(","));

			if(TimeUnit.DAYS.toString().equals(unit)) {
				return Duration.ofDays(amount);
			} else if (TimeUnit.HOURS.toString().equals(unit)) {
				return Duration.ofHours(amount);
			} else if(TimeUnit.MINUTES.toString().equals(unit)) {
				return Duration.ofMinutes(amount);
			} else if (TimeUnit.SECONDS.toString().equals(unit)) {
				return Duration.ofSeconds(amount);
			}
		}

		return Duration.ofHours(0);
	}	
	
	private String getCustomInfoByCode(TaskConfig task) {
		String wfEstimateCode = getWfEstimateLineFromCode(task, "WfEstimate.setCustomInfo");
		if (StringUtils.isNotEmpty(wfEstimateCode)) {
			String result = StringUtils.substringBetween(wfEstimateCode, "(\"", "\")");
			return result;
		}
		return null;
	}
	
	private boolean isSystemTask(TaskAndCaseModifier task) {		
		return task.getAllTaskConfigs().stream().anyMatch(it -> "SYSTEM".equals(it.getActivator().getName()));
	}
}
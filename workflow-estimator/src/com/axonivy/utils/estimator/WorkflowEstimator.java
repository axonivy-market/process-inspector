package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

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
import ch.ivyteam.ivy.process.model.NodeElement;
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

	public List<EstimatedTask> findAllTasks(BaseElement startAtElement) {
		List<EstimatedTask> estimatedTasks = emptyList();

		if (startAtElement instanceof NodeElement) {
			List<List<BaseElement>> paths = graph.findPaths(startAtElement);
			estimatedTasks = convertToEstimatedTasks(paths);			
		}
		
		return estimatedTasks;
	}

	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) throws Exception {
		List<EstimatedTask> estimatedTasks = emptyList();
		
		if (startAtElement instanceof NodeElement) {
			List<List<BaseElement>> paths = graph.findPaths(startAtElement, flowName);
			//Maybe throw an exception if there are not path
			if(paths.isEmpty()) {
				throw new Exception("Not found");
			}
			
			estimatedTasks = convertToEstimatedTasks(paths);
		}
		
		return estimatedTasks;
	}
	
	public Duration calculateEstimatedDuration(BaseElement startElement) {
		List<List<BaseElement>> paths = emptyList();
		if (startElement instanceof NodeElement && StringUtils.isNotEmpty(flowName)) {
			paths = graph.findPaths(startElement, flowName);					
		} else {
			paths = graph.findPaths(startElement);
		}
		
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(paths);
		Duration total = Duration.ofHours(0);
		for(EstimatedTask item : estimatedTasks) {
			total = total.plus(item.getEstimatedDuration());
		}
		return total;
	}
		
	private List<EstimatedTask> convertToEstimatedTasks(List<List<BaseElement>> paths) {
		
		List<TaskAndCaseModifier> taskCaseModifier = paths.stream()
				// If have more than one path, we get the longest
				.max(Comparator.comparingInt(List::size)).orElse(emptyList()).stream()
				// filter to get task only
				.filter(node -> {
					return node instanceof TaskAndCaseModifier;
				})
				.filter(node -> {
					return node instanceof RequestStart == false;
				})
				.map(TaskAndCaseModifier.class::cast)
				// filter the task which have estimated if needed
				.toList();
		//Convert to EstimatedTask
		List<EstimatedTask> estimatedTasks = new ArrayList<>(); 
		for(int i = 0; i < taskCaseModifier.size(); i ++) {
			List<EstimatedTask> estimatedTaskResults = new ArrayList<>();
			Date startTimestamp = i == 0 ? new Date() : estimatedTasks.get(i - 1).calculateEstimatedEndTimestamp();
			estimatedTaskResults = createEstimatedTask(taskCaseModifier.get(i), startTimestamp);
			
			estimatedTasks.addAll(estimatedTaskResults);
		}
		
		return estimatedTasks;
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
		
		return estimatedTasks;
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
}

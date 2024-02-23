package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

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
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.activity.UserTask;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;

@SuppressWarnings("restriction")
public class WorkflowEstimator {

	private Process process;
	private Enum<?> useCase;
	private String flowName;

	public WorkflowEstimator(Process process, Enum<?> useCase, String flowName) {
		this.process = process;
		this.useCase = useCase;
		this.flowName = flowName;
	}

	public List<EstimatedTask> findAllTasks(BaseElement startAtElement) {
		List<EstimatedTask> estimatedTasks = emptyList();

		if (startAtElement instanceof NodeElement) {
			List<List<BaseElement>> paths = findPaths(startAtElement);
			estimatedTasks = convertToEstimatedTasks(paths);			
		}
		
		return estimatedTasks;
	}

	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) {
		List<EstimatedTask> estimatedTasks = emptyList();
		
		if (startAtElement instanceof NodeElement) {
			List<List<BaseElement>> paths = findPaths(startAtElement, flowName);
			estimatedTasks = convertToEstimatedTasks(paths);	
		}
		
		return estimatedTasks;
	}

	public List<NodeElement> nextNodeElements(NodeElement from) {
		var nexts = new ArrayList<NodeElement>();

		if (from != null) {
			List<SequenceFlow> outs = from.getOutgoing();
			for (SequenceFlow out : outs) {
				NodeElement target = out.getTarget();
				List<NodeElement> nextElements = nextNodeElements(target);
				nexts.add(target);
				nexts.addAll(nextElements);
			}
		}

		return nexts.stream().distinct().toList();
	}
	
	public Duration calculateEstimatedDuration(BaseElement startElement) {
		List<List<BaseElement>> paths = emptyList();
		if (startElement instanceof NodeElement && flowName != null) {
			paths = findPaths(startElement, flowName);					
		} else {
			paths = findPaths(startElement);
		}
		
		List<EstimatedTask> estimatedTasks = convertToEstimatedTasks(paths);
		Duration total = Duration.ofHours(0);
		for(EstimatedTask item : estimatedTasks) {
			total = total.plus(item.getEstimatedDuration());
		}
		return total;
	}
	
	private List<List<BaseElement>> findPaths(BaseElement from) {
		return findPaths(from, null);
	}
	
	private List<EstimatedTask> convertToEstimatedTasks(List<List<BaseElement>> paths) {
		List<UserTask> tasks = paths.stream()
				// If have more than one path, we get the longest
				.max(Comparator.comparingInt(List::size)).orElse(emptyList()).stream()
				// filter to get task only
				.filter(node -> {
					return node instanceof UserTask;
				})
				.map(UserTask.class::cast)
				// filter the task which have estimated if needed
				.toList();
		//Convert to EstimatedTask
		List<EstimatedTask> estimatedTasks = new ArrayList<>(); 
		for(int i = 0; i < tasks.size(); i ++) {
			EstimatedTask estimatedTask = new EstimatedTask();
			Date startTimestamp = i == 0 ? new Date() : estimatedTasks.get(i - 1).calculateEstimatedEndTimestamp();
			estimatedTask = createEstimatedTask(tasks.get(i), startTimestamp);
			
			estimatedTasks.add(estimatedTask);
		}
		
		return estimatedTasks;
	}
	
	private List<List<BaseElement>> findPaths(BaseElement from, String flowName) {
		List<List<BaseElement>> paths = findNextNodeElementPath(from);
		paths.forEach(path -> {
			path.add(0, from);
		});

		List<List<BaseElement>> pathByFlowName = paths.stream()
				.filter(path -> hasFlowName(path, flowName))
				.toList();

		return pathByFlowName;
	}
	
	private List<List<BaseElement>> findNextNodeElementPath(BaseElement from) {
		var nexts = new ArrayList<List<BaseElement>>();

		if (from != null && from instanceof NodeElement) {
			List<SequenceFlow> outs = ((NodeElement) from).getOutgoing();
			for (SequenceFlow out : outs) {

				NodeElement target = out.getTarget();
				List<List<BaseElement>> nextElements = findNextNodeElementPath(target);

				if (nextElements.isEmpty()) {
					nextElements.add(new ArrayList<BaseElement>(Arrays.asList(target, out)));
				} else {
					nextElements.forEach(nodes -> {
						nodes.add(0, target);
						nodes.add(1, out);
					});
				}

				nexts.addAll(nextElements);
			}
		}
		return nexts;
	}
	
	private boolean hasFlowName(List<BaseElement> elements, String flowName) {
		if (StringUtils.isBlank(flowName)) {
			return true;
		}

		boolean existsFlowName = false;
		for (BaseElement el : elements) {
			if (el instanceof SequenceFlow) {
				String label = ((SequenceFlow) el).getEdge().getLabel().getText();
				if(StringUtils.isNotBlank(label)) {
					//Have label but not contain flowName -> Exit check
					if(!label.contains(flowName)) {
						return false;
					} else {
						existsFlowName = true;
					}
				}
			}
		}

		return existsFlowName;
	}
	
	private EstimatedTask createEstimatedTask(UserTask task, Date startTimestamp) {
		EstimatedTask estimatedTask = new EstimatedTask();
		estimatedTask.setPid(task.getPid().getRawPid());
		estimatedTask.setTaskName(task.getTaskConfig().getName().getRawMacro());
		estimatedTask.setParentElementNames(emptyList());
		
		Duration estimatedDuration = getDurationByCode(task.getTaskConfig());
		estimatedTask.setEstimatedDuration(estimatedDuration);
		estimatedTask.setEstimatedStartTimestamp(startTimestamp);

		return estimatedTask;
	}
	
	private Duration getDurationByCode(TaskConfig task) {
		// strongly typed!
		String script = Optional.of(task.getScript()).orElse(EMPTY);
		String[] codeLines = script.split("\\n");
		String wfEstimateCode = Arrays.stream(codeLines)
				.filter(line -> line.contains("WfEstimate."))
				.findFirst()
				.orElse(EMPTY);
		
		if (StringUtils.isNotEmpty(wfEstimateCode)) {
			String result = StringUtils.substringBetween(script, "(", "Use");
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
	
}

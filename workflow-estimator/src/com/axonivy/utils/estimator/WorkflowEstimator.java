package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.element.activity.UserTask;

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
			
			estimatedTasks = paths.stream() 
					//If have more than on path.			 
		            .min(Comparator.comparingInt(List::size))
		            .orElse(emptyList())
		            .stream()
					// filter to get task only
					.filter(node -> {
						return node instanceof UserTask;
					})
					// filter the task which have estimated if needed
					.map(task -> createEstimatedTask((UserTask) task))
					.toList();
		}

		return estimatedTasks;
	}

	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) {
		// TODO: Implement here
		return emptyList();
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
	
	private List<List<BaseElement>> findPaths(BaseElement from) {
		return findPaths(from, null);
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

		for (BaseElement el : elements) {
			if (el instanceof SequenceFlow) {
				String label = ((SequenceFlow) el).getEdge().getLabel().getText();
				if (label.contains(flowName)) {
					return true;
				}
			}
		}

		return false;
	}
	
	private EstimatedTask createEstimatedTask(UserTask task) {
		EstimatedTask estimatedTask = new EstimatedTask();
		estimatedTask.setPid(task.getPid().getRawPid());
		estimatedTask.setTaskName(task.getName());
		estimatedTask.setEstimatedStartTimestamp(new Date());

		return estimatedTask;
	}
}

package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

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
			List<NodeElement> nextNodes = nextNodeElements((NodeElement) startAtElement);

			estimatedTasks = Stream.concat(Stream.of(startAtElement), nextNodes.stream())
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

	private EstimatedTask createEstimatedTask(UserTask task) {
		EstimatedTask estimatedTask = new EstimatedTask();
		estimatedTask.setPid(task.getPid().getRawPid());
		estimatedTask.setTaskName(task.getName());
		estimatedTask.setEstimatedStartTimestamp(new Date());

		return estimatedTask;
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
}

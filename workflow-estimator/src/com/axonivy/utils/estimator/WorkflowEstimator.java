package com.axonivy.utils.estimator;

import static java.util.Collections.emptyList;

import java.util.List;

import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.bpm.engine.restricted.model.IProcess;
import ch.ivyteam.ivy.process.model.BaseElement;

public class WorkflowEstimator {
	public WorkflowEstimator(IProcess process, Enum<?> useCase, String flowName) {

	}

	public List<EstimatedTask> findAllTasks(BaseElement startAtElement) {
		// TODO: Implement here
		return emptyList();
	}

	public List<EstimatedTask> findTasksOnPath(BaseElement startAtElement) {
		// TODO: Implement here
		return emptyList();
	}
}

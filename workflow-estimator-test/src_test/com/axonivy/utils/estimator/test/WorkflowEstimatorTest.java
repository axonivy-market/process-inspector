package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@IvyTest
@SuppressWarnings("restriction")
public class WorkflowEstimatorTest {

	private Process process;	
	private ProcessGraph graph;

	@BeforeEach
	void setup() {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		this.process = manager.findProcessByPath("MainTest").getModel();
		this.graph = new ProcessGraph(process);	
	}

	@Test
	void shouldfindAllTasksAtStartRequestWithoutFlowName() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);

		var estimatedTasks = workflowEstimator.findAllTasks(graph.findStart());

		assertEquals(1, estimatedTasks.size());
		assertEquals("Task A", estimatedTasks.get(0).getTaskName());
	}
	
	@Test
	void shouldfindAllTasksAtTaskB() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskB = this.graph.findByElementName("Task B");
		var estimatedTasks = workflowEstimator.findAllTasks(taskB).stream()
				.sorted(Comparator.comparing(EstimatedTask::getTaskName))
				.toList();

		assertEquals(1, estimatedTasks.size());		
		assertEquals("Task B", estimatedTasks.get(0).getTaskName());		
	}
	
	@Test
	void shouldfindAllTasksAtTaskC() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskC = this.graph.findByElementName("Task C");
		var estimatedTasks = workflowEstimator.findAllTasks(taskC);

		assertEquals(2, estimatedTasks.size());
		assertEquals("Task C", estimatedTasks.get(0).getTaskName());
		assertEquals("Task B", estimatedTasks.get(1).getTaskName());		
	}
}

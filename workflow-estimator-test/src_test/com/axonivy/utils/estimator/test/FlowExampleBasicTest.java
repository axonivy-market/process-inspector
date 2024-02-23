package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.value.task.TaskConfig;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleBasicTest {

	private Process process;
	private ProcessGraph graph;

	@BeforeEach
	void setup() {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		this.process = manager.findProcessByPath("FlowExampleBasic").getModel();
		this.graph = new ProcessGraph(process);
	}

	@Test
	void shouldFindAllTasksAtStartRequestWithoutFlowName() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);

		var estimatedTasks = workflowEstimator.findAllTasks(graph.findStart());

		assertEquals(3, estimatedTasks.size());
		assertEquals("Task A", estimatedTasks.get(0).getTaskName());
		assertEquals("Task C", estimatedTasks.get(1).getTaskName());
		assertEquals("Task B", estimatedTasks.get(2).getTaskName());
	}

	@Test
	void shouldFfindAllTasksAtTaskB() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskB = this.graph.findByElementName("Task B");
		var estimatedTasks = workflowEstimator.findAllTasks(taskB);

		assertEquals(1, estimatedTasks.size());
		assertEquals("Task B", estimatedTasks.get(0).getTaskName());
	}

	@Test
	void shouldFindAllTasksAtTaskC() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskC = this.graph.findByElementName("Task C");
		var estimatedTasks = workflowEstimator.findAllTasks(taskC);

		assertEquals(2, estimatedTasks.size());
		assertEquals("Task C", estimatedTasks.get(0).getTaskName());
		assertEquals("Task B", estimatedTasks.get(1).getTaskName());
	}

	@Test
	void shouldFindAllTasksOfInternalFlow() {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(graph.findStart());

		assertEquals(2, estimatedTasks.size());
		assertEquals("Task A", estimatedTasks.get(0).getTaskName());
		assertEquals("Task B", estimatedTasks.get(1).getTaskName());
	}

	@Test
	void shouldFindAllTasksOfExternalFlow() {
		var workflowEstimator = new WorkflowEstimator(process, null, "external");
		var estimatedTasks = workflowEstimator.findTasksOnPath(graph.findStart());

		assertEquals(3, estimatedTasks.size());
		assertEquals("Task A", estimatedTasks.get(0).getTaskName());
		assertEquals("Task C", estimatedTasks.get(1).getTaskName());
		assertEquals("Task B", estimatedTasks.get(2).getTaskName());
	}
	
	@Test
	void shouldCalculateTotalDuration() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		Duration duration = workflowEstimator.calculateEstimatedDuration(graph.findStart());
		assertEquals(15, duration.toHours());
	}
}

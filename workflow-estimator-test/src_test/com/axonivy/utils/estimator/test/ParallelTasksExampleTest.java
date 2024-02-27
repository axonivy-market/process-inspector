package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class ParallelTasksExampleTest extends FlowExampleTest {
	
	private static BaseElement start;
	private static final String PROCESS_NAME = "ParallelTasksExample";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = graph.findByElementName("start");
	}
	
	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2", "Task3A", "Task3B"), names);
	}
	
	@Test
	void shouldFindAllTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2", "Task3A", "Task3B"), names);
	}
	
	@Test
	void shouldFindAllTasksAtStartWithFlowNameShortcut() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "shortcut");
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2"), names);
	}

}

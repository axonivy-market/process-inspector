package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
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
public class FlowExampleComplexTest extends FlowExampleTest {

	private static BaseElement start;
	private static BaseElement taskD;
	private static BaseElement taskE;
	private static final String PROCESS_NAME = "FlowExampleComplex";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = graph.findByElementName("start");
		taskD = graph.findByElementName("Task D");
		taskE = graph.findByElementName("Task E");
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);

		var expected = Arrays.array("Task A", "Task C", "Task1A", "Task1B", "Task D", "Task E", "Task2A", "Task2B",
				"Task G", "Task H", "Task F", "Task K", "Task B");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtTaskKAndTaskF() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskK = graph.findByElementName("Task K");
		var taskF = graph.findByElementName("Task F");
		
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(List.of(taskK, taskF));

		var expected = Arrays.array("Task K", "Task F", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtTaskD() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(taskD);

		var expected = Arrays.array("Task D", "Task E", "Task2A", "Task2B", "Task G", "Task H", "Task F", "Task K");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var expected = Arrays.array("Task A", "Task B", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskDAndTaskE() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(List.of(taskD, taskE));
		
		var expected = Arrays.array("Task D", "Task K", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldCalculateEstimateDurationBasedOnManyStartElements() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		Duration duration = workflowEstimator.calculateEstimatedDuration(List.of(taskD, taskE));
		
		assertEquals(Duration.ofHours(19), duration);
	}
}

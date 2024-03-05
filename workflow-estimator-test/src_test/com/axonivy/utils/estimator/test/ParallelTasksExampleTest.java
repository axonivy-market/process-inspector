package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
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
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2", "Task3A", "Task3B"), names);
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameShortcut() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "shortcut");
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2"), names);
	}
	
	@Test
	void shouldFindOverrideDuration() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		
		HashMap<String, Duration> hashMap = new HashMap<>();
		hashMap.put("18DD185B60B6E769-f15-TaskA", Duration.ofHours(10));
		workflowEstimator.setDurationOverrides(hashMap);
		
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);
		assertEquals(estimatedTasks.get(3).getEstimatedDuration().toHours(), 10);
	}
	
	@Test
	void shouldFindDefaultDuration() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);	
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		assertEquals(estimatedTasks.get(3).getEstimatedDuration().toHours(), 5);
	}

}

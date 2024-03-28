package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;

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
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2", "Task3B", "Task3A"), names);
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2", "Task3B", "Task3A"), names);
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameShortcut() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "shortcut");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("Task1A", "Task1B", "Task2"), names);
	}
	
	@Test
	void shouldFindOverrideDuration() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		
		HashMap<String, Duration> durationOverride = new HashMap<>(); 
		durationOverride.put("18DD185B60B6E769-f15-TaskA", Duration.ofHours(10));
		workflowEstimator.setDurationOverrides(durationOverride);
		
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
		var duration = estimatedTasks.stream()
				.filter(it -> it.getPid().contains("18DD185B60B6E769-f15-TaskA"))
				.findFirst()
				.map(it -> ((EstimatedTask)it).getEstimatedDuration())
				.orElse(null);
		assertEquals(duration.toHours(), 10);
	}
	
	@Test
	void shouldFindDefaultDuration() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);	
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		var duration = estimatedTasks.stream()
				.filter(it -> it.getPid().contains("18DD185B60B6E769-f15-TaskA"))
				.findFirst()
				.map(it -> ((EstimatedTask)it).getEstimatedDuration())
				.orElse(null);
		
		assertEquals(duration.toHours(), 5);
	}

}

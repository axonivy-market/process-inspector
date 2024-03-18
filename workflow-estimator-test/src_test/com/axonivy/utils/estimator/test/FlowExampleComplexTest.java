package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.assertj.core.util.Lists;
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
		start = ProcessGraphHelper.findByElementName(process, "start");
		taskD = ProcessGraphHelper.findByElementName(process, "Task D");
		taskE = ProcessGraphHelper.findByElementName(process, "Task E");
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);

		var expected = Lists.list("Task A", "Task C", "Task1A", "Task1B", "Task D", "Task E", "Task2A", "Task2B",
				"Task G", "Task H", "Task F", "Task K", "Task B");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames));
	}

	@Test
	void shouldFindAllTasksAtTaskKAndTaskF() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskK = ProcessGraphHelper.findByElementName(process, "Task K");
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");
		
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(List.of(taskK, taskF));

		var expected = Lists.list("Task K", "Task F", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames));
	}
	
	@Test
	void shouldFindAllTasksAtTaskFAndTaskE() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);		
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");
		
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(List.of(taskF, taskE));

		var expected = Lists.list("Task F", "Task E", "Task2A", "Task2B", "Task G", "Task H", "Task K");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames));
	}
	
	@Test
	void shouldFindAllTasksAtTaskD() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(taskD);

		var expected = Lists.list("Task D", "Task E", "Task2A", "Task2B", "Task G", "Task H", "Task F", "Task K");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		
		assertTrue(expected.containsAll(taskNames));
	}

	@Test
	void shouldFindTasksOnPathAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var expected = Lists.list("Task A", "Task B", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames));
	}

//	@Test
//	void shouldFindTasksOnPathWithoutFlowNameAtTaskDAndTaskE() throws Exception {
//		var workflowEstimator = new WorkflowEstimator(process, null, null);
//		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(List.of(taskD, taskE));
//		
//		var expected = Arrays.array("Task D", "Task2A", "Task2B", "Task G", "Task K", "Task H", "Task E");
//		var taskNames = getTaskNames(estimatedTasks);
//		assertArrayEquals(expected, taskNames);
//	}
	
//	@Test
//	void shouldCalculateEstimateDurationBasedOnManyStartElements() throws Exception {
//		var workflowEstimator = new WorkflowEstimator(process, null, null);
//		Duration duration = workflowEstimator.calculateEstimatedDuration(List.of(taskD, taskE));
//		
//		assertEquals(Duration.ofHours(19), duration);
//	}
	
//	@Test
//	void shouldFindAllTasksWitProcessFlowOverridesAtTaskE() throws Exception {
//		var workflowEstimator = new WorkflowEstimator(process, null, null);		
//		var flowOverrides = new HashMap<String, String>();
//		flowOverrides.put("18DF31B990019995-f47", "18DF31B990019995-f28");
//		flowOverrides.put("18DF31B990019995-f16", "18DF31B990019995-f21");
//		workflowEstimator.setProcessFlowOverrides(flowOverrides);
//				
//		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(taskE);
//
//		var expected = Arrays.array("Task E", "Task F");
//		var taskNames = getTaskNames(estimatedTasks);
//		assertArrayEquals(expected, taskNames);
//	}
}

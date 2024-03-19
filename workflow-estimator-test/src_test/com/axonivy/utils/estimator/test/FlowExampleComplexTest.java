package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleComplexTest extends FlowExampleTest {

	private static BaseElement start;
	private static BaseElement taskB;
	private static BaseElement taskC;
	private static final String PROCESS_NAME = "FlowExampleComplex";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
		taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		taskC = ProcessGraphHelper.findByElementName(process, "Task C");
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(start);

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
		
		var estimatedTasks = workflowEstimator.findAllTasks(List.of(taskK, taskF));

		var expected = Lists.list("Task K", "Task F", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames));
	}
	
	@Test
	void shouldFindAllTasksAtTaskFAndTaskB() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);		
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");
		
		var estimatedTasks = workflowEstimator.findAllTasks(List.of(taskF, taskB));
		
		var expected = Lists.list("Task F", "Task2A", "Task2B", "Task G", "Task H", "Task K", "Task B");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames) && taskNames.containsAll(expected));
	}
	
	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(taskC);

		var expected = Lists.list("Task C", "Task1A", "Task1B", "Task D", "Task E", "Task2A", "Task2B", "Task G", "Task K", "Task H", "Task F");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		
		assertTrue(expected.containsAll(taskNames) && taskNames.containsAll(expected));
	}

	@Test
	void shouldFindTasksOnPathAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		var expected = Lists.list("Task A", "Task B", "Task E", "Task2A", "Task2B", "Task G", "Task H");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames));
	}

	@Test
	void shouldCalculateEstimateDurationBasedOnManyStartElements() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		Duration duration = workflowEstimator.calculateEstimatedDuration(List.of(taskB, taskC));
		
		assertEquals(Duration.ofHours(29), duration);
	}
	
	@Test
	void shouldFindTasksOnPathWithProcessFlowOverridesAtTaskC() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);		
		var flowOverrides = new HashMap<String, String>();
		flowOverrides.put("18DF31B990019995-f47", "18DF31B990019995-f28");
		workflowEstimator.setProcessFlowOverrides(flowOverrides);
				
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskC);

		var expected = Lists.list("Task C", "Task1A", "Task1B", "Task D", "Task E", "Task F",  "Task K");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames) && taskNames.containsAll(expected));
	}
	
	@Test
	void shouldFindTasksOnPathAtTaskD() throws Exception {
		//Can not run with the case with start element after TaskSwichGetway
	}
}

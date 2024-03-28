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
		
		var expected = Arrays.array("Task A", "Task B", "Task K", "Task2A", "Task H", "Task2B", "Task G", "Task F",
				"Task C", "Task1A", "Task E", "Task1B", "Task D");
		var taskNames = (getTaskNames(estimatedTasks));
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtTaskKAndTaskF() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var taskK = ProcessGraphHelper.findByElementName(process, "Task K");
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");
		
		var estimatedTasks = workflowEstimator.findAllTasks(List.of(taskK, taskF));
		
		var expected = Arrays.array("Task K", "Task F", "Task2A", "Task H", "Task2B", "Task G" );
		var taskNames = (getTaskNames(estimatedTasks));
		
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtTaskFAndTaskB() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);		
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");
		
		var estimatedTasks = workflowEstimator.findAllTasks(List.of(taskF, taskB));
		
		var expected = Arrays.array("Task F", "Task K", "Task2A", "Task H", "Task2B", "Task G", "Task B");
		var taskNames = (getTaskNames(estimatedTasks));
		
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(taskC);
		//TODO: Should fix this case. Remote last Task K
		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task2A", "Task H", "Task2B",
				"Task G", "Task K", "Task F", "Task K");
		var taskNames = (getTaskNames(estimatedTasks));

		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskCWithInternal() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskC);
		
		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task2A", "Task H", "Task2B", "Task G");
		var taskNames = (getTaskNames(estimatedTasks));

		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtTaskC() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskC);
		
		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task2A", "Task H", "Task2B",
				"Task G", "Task K");
		var taskNames = (getTaskNames(estimatedTasks));

		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		var expected = Arrays.array("Task A", "Task B", "Task2A", "Task H","Task2B", "Task G");
		var taskNames = (getTaskNames(estimatedTasks));
		assertArrayEquals(expected, taskNames);
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
		
		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task F",  "Task K");
		var taskNames = (getTaskNames(estimatedTasks));
		
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtTaskD() throws Exception {
		//Can not run with the case with start element after TaskSwichGetway
	}
}

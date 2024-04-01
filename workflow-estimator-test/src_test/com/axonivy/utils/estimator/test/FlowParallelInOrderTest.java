package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.assertj.core.util.Arrays;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.constant.UseCase;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
@SuppressWarnings("restriction")
public class FlowParallelInOrderTest extends FlowExampleTest {
		
	private static final String PROCESS_NAME = "FlowParallelInOrder";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}
	
	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		var expected = Arrays.array("Task 1A", "Task A", "Task B", "Task 1B",  "Task C", "Task D");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtStart2() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		var estimatedTasks = workflowEstimator.findAllTasks(start2);
		
		var expected = Arrays.array("Task 2B", "Task G", "Task K", "Task M", "Task 2A", "Task F", "Task H", "Task 2C", "Task E", "Task I");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtStart3() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var estimatedTasks = workflowEstimator.findAllTasks(start3);		
		
		var expected = Arrays.array("Task1A", "Task B", "Task1B", "Task A", "Task2B", "Task D", "Task2A", "Task C", "Task F", "Task K", "Task2C", "Task E", "Task3A", "Task I");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtStart3() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start3);		
		
		var expected = Arrays.array("Task1A", "Task B", "Task1B", "Task A", "Task2B", "Task D", "Task2A", "Task C", "Task K", "Task2C", "Task3A", "Task I");
		var taskNames = getTaskNames(estimatedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, UseCase.SMALLPROJECT, null);
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		Duration duration = workflowEstimator.calculateEstimatedDuration(start2);
		assertEquals(10, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithBIGPROJECT() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, UseCase.BIGPROJECT, null);
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		Duration duration = workflowEstimator.calculateEstimatedDuration(start2);
		assertEquals(15, duration.toHours());
	}
}
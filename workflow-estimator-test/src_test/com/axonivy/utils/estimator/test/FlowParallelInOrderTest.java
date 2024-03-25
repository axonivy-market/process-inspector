package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

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

		var expected = Lists.list("Task 1A", "Task 1B", "Task A", "Task B", "Task C", "Task D");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtStart2() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		var estimatedTasks = workflowEstimator.findAllTasks(start2);
		
		var expected = Lists.list("Task 2A", "Task 2B", "Task 2C", "Task F", "Task H", "Task G", "Task K", "Task M", "Task E", "Task I");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtStart3() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var estimatedTasks = workflowEstimator.findAllTasks(start3);		
		
		var expected = Lists.list("Task1A", "Task1B", "Task B", "Task A", "Task2A", "Task2B", "Task2C", "Task D", "Task C", "Task F", "Task K", "Task E", "Task3A");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtStart3() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start3);		
		
		var expected = Lists.list("Task1A", "Task1B", "Task B", "Task A", "Task2A", "Task2B", "Task2C", "Task D", "Task C", "Task K", "Task3A");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertEquals(expected, taskNames);
	}
}
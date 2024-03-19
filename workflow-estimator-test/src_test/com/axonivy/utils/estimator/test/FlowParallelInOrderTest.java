package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowParallelInOrderTest extends FlowExampleTest {
	
	private static BaseElement start;
	
	private static final String PROCESS_NAME = "FlowParallelInOrder";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		var expected = Lists.list("Task 1A", "Task 1B", "Task A", "Task B", "Task C", "Task D");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtStart2() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		start = ProcessGraphHelper.findByElementName(process, "start2");
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		var expected = Lists.list("Task 2A", "Task 2B", "Task 2C", "Task G", "Task K", "Task M", "Task F", "Task H", "Task E", "Task I");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertEquals(expected, taskNames);
	}

}
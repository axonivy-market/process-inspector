package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class TaskTypesExampleTest extends FlowExampleTest {
	
	private static BaseElement start;
	private static final String PROCESS_NAME = "TaskTypesExample";
	
	@BeforeAll	
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindAllTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("UserTask", "Task", "Tasks-TaskA", "Tasks-TaskB"), names);
	}
	
	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(start);
		
		var names = getTaskNames(estimatedTasks);
		assertArrayEquals(Arrays.array("UserTask", "Task", "Tasks-TaskA", "Tasks-TaskB"), names);
	}

}

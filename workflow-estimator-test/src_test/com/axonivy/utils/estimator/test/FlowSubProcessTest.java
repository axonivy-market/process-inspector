package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowSubProcessTest extends FlowExampleTest {
	
	private static BaseElement start;
	private static final String PROCESS_NAME = "FlowSubprocess";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = graph.findByElementName("start");
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);
		
		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTaskParentNames() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);
		Optional<EstimatedTask> taskA = estimatedTasks.stream().filter(item -> item.getTaskName().equals("Task A")).findFirst();
		
		assertEquals(java.util.Arrays.asList("sub with two levels", "2nd level sub") , taskA.get().getParentElementNames());
	}

}
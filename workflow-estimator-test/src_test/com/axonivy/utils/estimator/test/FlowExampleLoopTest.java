package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
public class FlowExampleLoopTest extends FlowExampleTest {

	private static BaseElement start;
	private static final String PROCESS_NAME = "FlowExampleLoop";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindAllTasksStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameSuccess() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "success");
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A"), getTaskNames(estimatedTasks));
	}
	
}

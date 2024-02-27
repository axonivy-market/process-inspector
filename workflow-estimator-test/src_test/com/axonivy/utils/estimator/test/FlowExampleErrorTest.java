package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleErrorTest extends FlowExampleTest {

	private static BaseElement start;
	private static final String PROCESS_NAME = "FlowExampleError";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);	
		start = graph.findByElementName("start");
	}
	
	@Test
	void shouldFindAllTasksOnPathAtStartWithFlowNameSuccess() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "success");
		List<EstimatedTask> estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertEquals(1, estimatedTasks.size());
		assertEquals("Task A", getTaskNames(estimatedTasks)[0]);
	}
	
	@Test
	void shouldFindAllTasksOnPathAtStartWithFlowNameNull()  {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		
		Exception exception = assertThrows(Exception.class, () -> {
			workflowEstimator.findTasksOnPath(start);
	    });

	    String expectedMessage = "Not found";
	    String actualMessage = exception.getMessage();

	    assertEquals(expectedMessage, actualMessage);
	}
	
}

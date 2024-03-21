package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;

import org.assertj.core.util.Arrays;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleBasicTest extends FlowExampleTest {

	private static BaseElement start;
	private static BaseElement newStart;
	private static BaseElement taskB;
	private static BaseElement taskC;
	private static final String PROCESS_NAME = "FlowExampleBasic";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
		newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		taskC = ProcessGraphHelper.findByElementName(process, "Task C");
	}
	
	@Test
	void shouldFindAllTasksAtStartIncludeGateway() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		workflowEstimator.enableDescribeAlternativeElements();
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Alter", "int/ext?", "Task C", "Task B", "Alter2"), getTaskNames(estimatedTasks));
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(estimatedTasks));
	}

	@Test
	void shouldFfindAllTasksAtTaskB() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(taskB);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));	
	}

	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(taskC);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));
	}

	@Test
	void shouldFindAllTasksAtNewStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(newStart);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
	
		assertArrayEquals(Arrays.array("Task A"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskB() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskB);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskC() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskC);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtNewStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithProcessFlowOverridesAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);		
		var flowOverrides = new HashMap<String, String>();
		flowOverrides.put("18DC44E096FDFF75-f8", "18DC44E096FDFF75-f12");
		workflowEstimator.setProcessFlowOverrides(flowOverrides);
				
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);

		var expected = Lists.list("Task C",  "Task B");
		var taskNames = Lists.list(getTaskNames(estimatedTasks));
		assertTrue(expected.containsAll(taskNames) && taskNames.containsAll(expected));
	}
	
	@Test
	void shouldFindAllTasksOfInternalFlowAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(estimatedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtNewStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);
		
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindAllTasksOfExternalFlowAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "external");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfExternalFlowAtNewStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "external");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfMixedFlowAtStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "mixed");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfMixedFlowAtNewStart() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "mixed");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldCalculateTotalDurationWithDefault() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		Duration duration = workflowEstimator.calculateEstimatedDuration(start);
		assertEquals(15, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, UseCase.SMALLPROJECT, null);
		Duration duration = workflowEstimator.calculateEstimatedDuration(start);
		assertEquals(5, duration.toHours());
	}
	
	@Test
	void shouldCheckCustomInfo() throws Exception {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);
		assertEquals("abc", ((EstimatedTask)estimatedTasks.get(0)).getCustomInfo());
	}
}

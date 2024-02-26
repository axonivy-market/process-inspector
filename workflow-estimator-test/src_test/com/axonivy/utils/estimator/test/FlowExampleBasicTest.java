package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleBasicTest {

	private static Process process;
	private static ProcessGraph graph;
	private static BaseElement start;
	private static BaseElement newStart;
	private static BaseElement taskB;
	private static BaseElement taskC;
	
	@BeforeAll
	public static void setup() {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		process = manager.findProcessByPath("FlowExampleBasic").getModel();
		graph = new ProcessGraph(process);
		
		start = graph.findByElementName("start");
		newStart = graph.findByElementName("NewStart");
		taskB = graph.findByElementName("Task B");
		taskC = graph.findByElementName("Task C");
	}

	@Test
	void shouldFindAllTasksAtStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		List<EstimatedTask> estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(estimatedTasks));
	}

	@Test
	void shouldFfindAllTasksAtTaskB() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(taskB);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));	
	}

	@Test
	void shouldFindAllTasksAtTaskC() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(taskC);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));
	}

	@Test
	void shouldFindAllTasksAtNewStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(newStart);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
	
		assertArrayEquals(Arrays.array("Task A"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskB() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskB);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskC() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(taskC);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtNewStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindAllTasksOfInternalFlowAtStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(estimatedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtNewStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "internal");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);
		
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindAllTasksOfExternalFlowAtStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "external");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfExternalFlowAtNewStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "external");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindAllTasksOfMixedFlowAtStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "mixed");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));		
	}
	
	@Test
	void shouldFindAllTasksOfMixedFlowAtNewStart() {
		var workflowEstimator = new WorkflowEstimator(process, null, "mixed");
		var estimatedTasks = workflowEstimator.findTasksOnPath(newStart);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(estimatedTasks));		
	}
	
	private String[] getTaskNames(List<EstimatedTask> tasks ) {
		return tasks.stream().map(EstimatedTask::getTaskName).toArray(String[]::new);
	}
	
	@Test
	void shouldCalculateTotalDuration() {
		var workflowEstimator = new WorkflowEstimator(process, null, null);
		Duration duration = workflowEstimator.calculateEstimatedDuration(graph.findStart());
		assertEquals(15, duration.toHours());
	}
}

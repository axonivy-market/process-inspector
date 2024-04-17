package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import com.axonivy.utils.process.analyzer.model.DetectedTask;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;

@IvyProcessTest
public class FlowParallelInOrderCaseTest extends FlowExampleTest {
	private static final BpmProcess FLOW_PARALLEL_IN_ORDER = BpmProcess.name("FlowParallelInOrder");
	
	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new AdvancedProcessAnalyzer(process);	
	}
	
	@Test
	void shouldshouldFindAllTasksAtStart(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_PARALLEL_IN_ORDER.elementName("start")).execute();
		ICase icase = result.workflow().activeCase();

		var detectedTasks = processAnalyzer.findAllTasks(icase, null);

		var expected = Arrays.array("Task 1A", "Task A", "Task B", "Task 1B", "Task C", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathByCaseAtStart3(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_PARALLEL_IN_ORDER.elementName("start3")).execute();
		ICase icase = result.workflow().activeCase();

		var detectedTasks = processAnalyzer.findTasksOnPath(icase, null, "internal");		
		
		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2B3", "Task D3", "Task2A3",
				"Task C3", "Task K3", "Task2C3", "Task G3", "Task3A3", "Task I3");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathByCaseAtTaskBAndTaskC(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_PARALLEL_IN_ORDER.elementName("start")).execute();
		List<ITask> parallelTasks = result.workflow().activeTasks();
		for(ITask task : parallelTasks) {
			result = bpmClient.start().task(task).as().everybody().execute();
		}
						
		List<ITask> activeTasks = result.workflow().activeTasks();
		ITask taskA = findTaskByElementName(activeTasks, "Task A");

		bpmClient.mock().uiOf(FLOW_PARALLEL_IN_ORDER.elementName("Task A")).withNoAction();		
		result = bpmClient.start().task(taskA).as().everybody().execute();
		Thread.sleep(1000);
		
		ICase icase = result.workflow().activeCase();

		var detectedTasks = processAnalyzer.findTasksOnPath(icase, null, null);		
		
		var expected = Arrays.array("Task C", "Task B", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
		
		DetectedTask taskC = (DetectedTask) findByElementName(detectedTasks, "Task C");
		DetectedTask taskD = (DetectedTask) findByElementName(detectedTasks, "Task D");
		
		assertTrue(Duration.ZERO.compareTo(taskC.getTimeUntilStart()) >= 0);
		assertEquals(taskC.getTimeUntilEnd(), taskD.getTimeUntilStart());
		assertEquals(taskC.getTimeUntilEnd().plus(taskD.getEstimatedDuration()), taskD.getTimeUntilEnd());
	}
}
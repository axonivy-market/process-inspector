package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.workflow.ICase;

@IvyProcessTest
@SuppressWarnings("restriction")
public class FlowParallelInOrderCaseTest extends FlowExampleTest {
	private static final BpmProcess FLOW_PARALLEL_IN_ORDER = BpmProcess.name("FlowParallelInOrder");
	private static final BpmElement FLOW_PARALLEL_IN_ORDER_START = FLOW_PARALLEL_IN_ORDER.elementName("start");
	private static final BpmElement FLOW_PARALLEL_IN_ORDER_START2 = FLOW_PARALLEL_IN_ORDER.elementName("start2");
	private static final BpmElement FLOW_PARALLEL_IN_ORDER_START3 = FLOW_PARALLEL_IN_ORDER.elementName("start3");

	@Test
	void shouldshouldFindAllTasksAtStart(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_PARALLEL_IN_ORDER_START).execute();
		ICase icase = result.workflow().activeCase();

		var processAnalyzer = new AdvancedProcessAnalyzer(getProcess(icase), null, null);
		var detectedTasks = processAnalyzer.findAllTasks(icase);

		var expected = Arrays.array("Task 1A", "Task A", "Task B", "Task 1B", "Task C", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtStart2(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_PARALLEL_IN_ORDER_START2).execute();
		ICase icase = result.workflow().activeCase();

		var processAnalyzer = new AdvancedProcessAnalyzer(getProcess(icase), null, null);
		var detectedTasks = processAnalyzer.findAllTasks(icase);
		
		var expected = Arrays.array("Task 2B", "Task G", "Task K", "Task M", "Task 2A", "Task F", "Task H", "Task 2C", "Task E", "Task I");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtStart3(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_PARALLEL_IN_ORDER_START3).execute();
		ICase icase = result.workflow().activeCase();

		var processAnalyzer = new AdvancedProcessAnalyzer(getProcess(icase), null, "internal");
		var detectedTasks = processAnalyzer.findTasksOnPath(icase);		
		
		var expected = Arrays.array("Task1A", "Task B", "Task1B", "Task A", "Task2B", "Task D", "Task2A", "Task C", "Task K", "Task2C", "Task3A", "Task I");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}
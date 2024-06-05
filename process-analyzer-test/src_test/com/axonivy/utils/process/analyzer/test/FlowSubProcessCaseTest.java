package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.workflow.ICase;

@IvyProcessTest
public class FlowSubProcessCaseTest extends FlowExampleTest {
	private static final BpmProcess FLOW_SUB_PROCESS = BpmProcess.name("FlowSubprocess");
		
	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new ProcessAnalyzer();
	}

	@Test
	void shouldFindAllTasksAtStart3(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_SUB_PROCESS.elementName("start3")).execute();
		ICase icase = result.workflow().activeCase();
		
		var detectedTasks = processAnalyzer.findAllTasks(icase, null);

		var expected = Arrays.array("CallSubProcess A", "TaskC", "TaskB");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtStart(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_SUB_PROCESS.elementName("start")).execute();
		ICase icase = result.workflow().activeCase();

		var detectedTasks = processAnalyzer.findAllTasks(icase, null);

		var expected = Arrays.array("Task A", "Task B");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldCalculateWorstCaseDuration(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_SUB_PROCESS.elementName("start")).execute();
		ICase icase = result.workflow().activeCase();
		
		var total = processAnalyzer.calculateWorstCaseDuration(icase, UseCase.BIGPROJECT);
		
		assertEquals(Duration.ofHours(9), total);
	}
}

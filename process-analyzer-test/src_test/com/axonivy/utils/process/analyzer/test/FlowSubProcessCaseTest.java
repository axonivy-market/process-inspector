package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;

@IvyProcessTest
public class FlowSubProcessCaseTest extends FlowExampleTest {
	private static final BpmProcess FLOW_SUB_PROCESS = BpmProcess.name("FlowSubprocess");
		
	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new ProcessAnalyzer();
	}

	@Test
	void shouldshouldFindAllTasksAtStart3(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_SUB_PROCESS.elementName("start3")).execute();
		ICase icase = result.workflow().activeCase();

		List<ITask> parallelTasks = result.workflow().activeTasks();
		
		var detectedTasks = processAnalyzer.findAllTasks(icase, null);

		var expected = Arrays.array("CallSubProcess A", "TaskC", "TaskB");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldshouldFindAllTasksAtStart(BpmClient bpmClient) throws Exception {
		ExecutionResult result = bpmClient.start().process(FLOW_SUB_PROCESS.elementName("start")).execute();
		ICase icase = result.workflow().activeCase();

		List<ITask> parallelTasks = result.workflow().activeTasks();
		
		var detectedTasks = processAnalyzer.findAllTasks(icase, null);

		var expected = Arrays.array("Task A", "Task B");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}

package com.axonivy.utils.process.analyzer.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessGraph;
import com.axonivy.utils.process.analyzer.model.ElementTask;
import com.axonivy.utils.process.analyzer.test.ProcessGraphHelper;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;

@IvyTest
public class ProcessGraphTest extends InternalAbstractTest {
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	private static final String PARALLEL_TASKS_EXAMPLE = "ParallelTasksExample";
	private static ProcessGraph processGraph;
	
	@BeforeAll
	public static void setup() {
		processGraph = new ProcessGraph();
	}
	
	@Test
	void shouldGetTaskId() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var taskB = (SingleTaskCreator) ProcessGraphHelper.findByElementName(process, "Task B");
		var result = processGraph.createElementTask(taskB, taskB.getTaskConfig());
		var expected = ElementTask.createSingle("18DC44E096FDFF75-f7");	
		
		assertEquals(expected, result);
	}
	
	@Test
	void shouldGetTaskIdOfMultiTask() throws Exception {
		Process process = getProcessByName(PARALLEL_TASKS_EXAMPLE);
		var task1 = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task1");
		var result = processGraph.createElementTask(task1, task1.getAllTaskConfigs().get(0));
		var expected = ElementTask.createGateway("18DD185B60B6E769-f2", "TaskA");
		
		assertEquals(expected, result);
	}
	
	@Test
	void shouldIsSystemTask() throws Exception {
		Process process = getProcessByName(PARALLEL_TASKS_EXAMPLE);
		var joinTask = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Join");
		var result = processGraph.isSystemTask(joinTask);
		assertTrue(result);
	}
}

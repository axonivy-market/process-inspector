package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.ProcessGraph;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@IvyTest
@SuppressWarnings("restriction")
public class ProcessGraphTest {
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	private static final String PARALLEL_TASKS_EXAMPLE = "ParallelTasksExample";
	private static final String FLOW_SUB_PROCESS = "FlowSubprocess";
	
	private Process getProcessByName(String processName) {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		return manager.findProcessByPath(processName).getModel();		
	}
	
	@Test
	void shouldFindPathAtStart() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var processGraph = new ProcessGraph(process);
		var result = processGraph.findPath("internal", start);
		
		var expected = Arrays.asList(
				"RequestStartZ:start (18DC44E096FDFF75-f0)",
				"SequenceFlowZ:RequestStartZ->UserTaskZ",
				"UserTaskZ:Task A\n(Element Label) (18DC44E096FDFF75-f2)",
				"SequenceFlowZ:UserTaskZ->AlternativeZ",
				"AlternativeZ: (18DC44E096FDFF75-f4)", 
				"SequenceFlowZ:AlternativeZ->AlternativeZ", 
				"AlternativeZ:int/ext? (18DC44E096FDFF75-f8)",
				"SequenceFlowZ:AlternativeZ->UserTaskZ",
				"UserTaskZ:Task B (18DC44E096FDFF75-f7)",
				"SequenceFlowZ:UserTaskZ->AlternativeZ",
				"AlternativeZ: (18DC44E096FDFF75-f6)",
				"SequenceFlowZ:AlternativeZ->TaskEndZ",
				"TaskEndZ: (18DC44E096FDFF75-f1)");
		for(int i = 0; i <expected.size(); i++) {
			assertEquals(expected.get(i), result.get(i).toString());	
		}
	}
	
	@Test
	void shouldGetTaskId() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		TaskAndCaseModifier taskB = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task B");
		var processGraph = new ProcessGraph(process);
		var result = processGraph.getTaskId(taskB, taskB.getAllTaskConfigs().get(0));
		
		assertEquals("18DC44E096FDFF75-f7", result);
	}
	
	@Test
	void shouldGetTaskIdOfMultiTask() throws Exception {
		Process process = getProcessByName(PARALLEL_TASKS_EXAMPLE);
		TaskAndCaseModifier task1 = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task1");
		var processGraph = new ProcessGraph(process);
		var result = processGraph.getTaskId(task1, task1.getAllTaskConfigs().get(0));
		
		assertEquals("18DD185B60B6E769-f2-TaskA", result);
	}
	
	@Test
	void shouldIsSystemTask() throws Exception {
		Process process = getProcessByName(PARALLEL_TASKS_EXAMPLE);
		TaskAndCaseModifier joinTask = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Join");
		var processGraph = new ProcessGraph(process);
		var result = processGraph.isSystemTask(joinTask);
		
		assertTrue(result);
	}
}
 
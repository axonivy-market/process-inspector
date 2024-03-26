package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.internal.model.CommonElement;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@IvyTest
@SuppressWarnings("restriction")
public class WorkflowImplementTest{
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	private static final String PARALLEL_TASKS_EXAMPLE = "ParallelTasksExample";
	private static final String FLOW_SUB_PROCESS = "FlowSubprocess";
	
	private static WorkflowImplement workflowImplement;
	
	private Process getProcessByName(String processName) {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		return manager.findProcessByPath(processName).getModel();		
	}
	
	@BeforeAll
	public static void setup() {
		workflowImplement = new WorkflowImplement();
	}

	@Test
	void shouldFindPathAtStart() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var start = ProcessGraphHelper.findByElementName(process, "start");		
		var result = workflowImplement.findPath("internal", new CommonElement(start));
		
		var expected = Arrays.asList(
				"RequestStartZ:start (18DC44E096FDFF75-f0)",
				"SequenceFlowZ:RequestStartZ->UserTaskZ",
				"UserTaskZ:Task A\n(Element Label) (18DC44E096FDFF75-f2)",
				"SequenceFlowZ:UserTaskZ->AlternativeZ",
				"AlternativeZ:Alter (18DC44E096FDFF75-f4)", 
				"SequenceFlowZ:AlternativeZ->AlternativeZ", 
				"AlternativeZ:int/ext? (18DC44E096FDFF75-f8)",
				"SequenceFlowZ:AlternativeZ->UserTaskZ",
				"UserTaskZ:Task B (18DC44E096FDFF75-f7)",
				"SequenceFlowZ:UserTaskZ->AlternativeZ",
				"AlternativeZ:Alter2 (18DC44E096FDFF75-f6)",
				"SequenceFlowZ:AlternativeZ->TaskEndZ",
				"TaskEndZ: (18DC44E096FDFF75-f1)");
		for(int i = 0; i <expected.size(); i++) {
			assertEquals(expected.get(i), result.get(i).toString());	
		}
	}
	
	@Test
	void shouldGetTaskId() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var taskB = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task B");		
		var result = workflowImplement.getTaskId(taskB, taskB.getAllTaskConfigs().get(0));
		
		assertEquals("18DC44E096FDFF75-f7", result);
	}
	
	@Test
	void shouldGetTaskIdOfMultiTask() throws Exception {
		Process process = getProcessByName(PARALLEL_TASKS_EXAMPLE);
		var task1 = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task1");
		var result = workflowImplement.getTaskId(task1, task1.getAllTaskConfigs().get(0));
		
		assertEquals("18DD185B60B6E769-f2-TaskA", result);
	}
	
	@Test
	void shouldIsSystemTask() throws Exception {
		Process process = getProcessByName(PARALLEL_TASKS_EXAMPLE);
		var joinTask = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Join");
		var result = workflowImplement.isSystemTask(joinTask);
		assertTrue(result);
	}
	
	@Test
	void shouldGetParentElementNames() throws Exception {
		Process process = getProcessByName(FLOW_SUB_PROCESS);
		var joinTask = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task A");		
		var result = workflowImplement.getParentElementNames(joinTask);
		
		assertEquals("[sub with two levels, 2nd level sub]", result.toString());
	}
	
	@Test
	void shouldGetDurationOfTaskCWithUseCaseBIGPROJECT() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var taskC = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task C");		
		var result = workflowImplement.getDuration(taskC, taskC.getAllTaskConfigs().get(0), UseCase.BIGPROJECT);
		
		assertEquals(Duration.ofHours(4),  result);
	}
	
	@Test
	void shouldGetDurationOfTaskCWithUseCaseMEDIUMPROJECT() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		TaskAndCaseModifier taskC = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task C");	
		var result = workflowImplement.getDuration(taskC, taskC.getAllTaskConfigs().get(0), UseCase.MEDIUMPROJECT);
		
		assertEquals(Duration.ofHours(3),  result);
	}
	
	@Test
	void shouldGetDurationOfTaskCWithUseCaseSMALLPROJECT() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		TaskAndCaseModifier taskC = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task C");
		var result = workflowImplement.getDuration(taskC, taskC.getAllTaskConfigs().get(0), UseCase.SMALLPROJECT);
		
		assertEquals(Duration.ofHours(2),  result);
	}
}
 
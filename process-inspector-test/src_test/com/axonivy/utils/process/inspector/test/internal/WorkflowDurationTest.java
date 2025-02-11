package com.axonivy.utils.process.inspector.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.inspector.internal.WorkflowDuration;
import com.axonivy.utils.process.inspector.model.ElementTask;
import com.axonivy.utils.process.inspector.test.ProcessGraphHelper;
import com.axonivy.utils.process.inspector.test.UseCase;
import com.axonivy.utils.process.inspector.utils.ProcessInspectorUtils;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;

@IvyTest
public class WorkflowDurationTest {
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	private static WorkflowDuration workflowDuration;

	@BeforeAll
	public static void setup() {
		workflowDuration = new WorkflowDuration();
	}

	@Test
	void shouldGetDurationOfTaskCWithUseCaseBIGPROJECT() throws Exception {
		Process process = ProcessInspectorUtils.getProcessByName(FLOW_EXAMPLE_BASIC);
		var taskC = (SingleTaskCreator) ProcessGraphHelper.findByElementName(process, "Task C");
		ElementTask elementTask = ElementTask.createSingle(taskC.getPid().getRawPid());
		var result = workflowDuration.getDuration(elementTask, taskC.getTaskConfig().getScript(), UseCase.BIGPROJECT);

		assertEquals(Duration.ofHours(4), result);
	}

	@Test
	void shouldGetDurationOfTaskCWithUseCaseMEDIUMPROJECT() throws Exception {
		Process process = ProcessInspectorUtils.getProcessByName(FLOW_EXAMPLE_BASIC);
		SingleTaskCreator taskC = (SingleTaskCreator) ProcessGraphHelper.findByElementName(process, "Task C");
		ElementTask elementTask = ElementTask.createSingle(taskC.getPid().getRawPid());
		var result = workflowDuration.getDuration(elementTask, taskC.getTaskConfig().getScript(),
				UseCase.MEDIUMPROJECT);

		assertEquals(Duration.ofHours(3), result);
	}

	@Test
	void shouldGetDurationOfTaskCWithUseCaseSMALLPROJECT() throws Exception {
		Process process = ProcessInspectorUtils.getProcessByName(FLOW_EXAMPLE_BASIC);
		SingleTaskCreator taskC = (SingleTaskCreator) ProcessGraphHelper.findByElementName(process, "Task C");
		ElementTask elementTask = ElementTask.createSingle(taskC.getPid().getRawPid());
		var result = workflowDuration.getDuration(elementTask, taskC.getTaskConfig().getScript(), UseCase.SMALLPROJECT);

		assertEquals(Duration.ofHours(2), result);
	}

}

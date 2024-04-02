package com.axonivy.utils.estimator.test.internal;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.constant.UseCase;
import com.axonivy.utils.estimator.internal.WorkflowPath;
import com.axonivy.utils.estimator.internal.WorkflowTime;
import com.axonivy.utils.estimator.test.ProcessGraphHelper;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskAndCaseModifier;

@IvyTest
@SuppressWarnings("restriction")
public class WorkflowTimeTest extends InternalAbstractTest {
	private static final String FLOW_EXAMPLE_BASIC = "FlowExampleBasic";
	private static WorkflowTime workflowTime;

	@BeforeAll
	public static void setup() {
		workflowTime = new WorkflowTime(emptyMap());
	}

	@Test
	void shouldGetDurationOfTaskCWithUseCaseBIGPROJECT() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		var taskC = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task C");
		var result = workflowTime.getDuration(taskC, taskC.getAllTaskConfigs().get(0), UseCase.BIGPROJECT);

		assertEquals(Duration.ofHours(4), result);
	}

	@Test
	void shouldGetDurationOfTaskCWithUseCaseMEDIUMPROJECT() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		TaskAndCaseModifier taskC = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task C");
		var result = workflowTime.getDuration(taskC, taskC.getAllTaskConfigs().get(0), UseCase.MEDIUMPROJECT);

		assertEquals(Duration.ofHours(3), result);
	}

	@Test
	void shouldGetDurationOfTaskCWithUseCaseSMALLPROJECT() throws Exception {
		Process process = getProcessByName(FLOW_EXAMPLE_BASIC);
		TaskAndCaseModifier taskC = (TaskAndCaseModifier) ProcessGraphHelper.findByElementName(process, "Task C");
		var result = workflowTime.getDuration(taskC, taskC.getAllTaskConfigs().get(0), UseCase.SMALLPROJECT);

		assertEquals(Duration.ofHours(2), result);
	}
}

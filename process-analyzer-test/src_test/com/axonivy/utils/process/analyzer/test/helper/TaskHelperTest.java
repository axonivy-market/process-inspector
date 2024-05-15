package com.axonivy.utils.process.analyzer.test.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.helper.TaskHelper;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.workflow.ITask;

@IvyProcessTest
public class TaskHelperTest {
	private static final BpmProcess FLOW_EXAMPLE_COMMON = BpmProcess.name("FlowExampleCommon");
	private static final BpmElement FLOW_EXAMPLE_COMMON_START = FLOW_EXAMPLE_COMMON.elementName("start");
	private static final BpmElement FLOW_EXAMPLE_COMMON_TASKA = FLOW_EXAMPLE_COMMON.elementName("TaskA");

	@Test
	void shouldGetBaseElementOfTaskB(BpmClient bpmClient) {
		ExecutionResult result = bpmClient.start().process(FLOW_EXAMPLE_COMMON_START).execute();
		ITask startTask = result.workflow().executedTask();

		BaseElement startElement = TaskHelper.getBaseElementOf(startTask);
		assertNotNull(startElement);
		assertEquals("start", startElement.getName());

		bpmClient.mock().uiOf(FLOW_EXAMPLE_COMMON_TASKA).withNoAction();

		result = bpmClient.start().anyActiveTask(result).as().everybody().execute();
		ITask taskA = result.workflow().executedTask();

		BaseElement taskAElement = TaskHelper.getBaseElementOf(taskA);
		assertNotNull(taskA);
		assertEquals("TaskA", taskAElement.getName());
	}

	@Test
	void shouldGetBaseElementOfIsNull(BpmClient bpmClient) {

		BaseElement startElement = TaskHelper.getBaseElementOf(null);
		assertNull(startElement);
	}
}

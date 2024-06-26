package com.axonivy.utils.process.inspector.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.inspector.internal.AdvancedProcessInspector;
import com.axonivy.utils.process.inspector.model.DetectedTask;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FlowExampleBasicTest extends FlowExampleTest {
	private static final String PROCESS_NAME = "FlowExampleBasic";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processInspector = new AdvancedProcessInspector();
	}

	@Test
	void shouldFindAllTasksAtStartIncludeAlternative() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		processInspector.enableDescribeAlternativeElements();
		var detectedTasks = processInspector.findAllTasks(start, UseCase.BIGPROJECT);

		var expected = Arrays.array("Task A", "Alter", "int/ext?", "Alter2", "Task C", "Task B");
		assertArrayEquals(expected, getElementNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findAllTasks(start, UseCase.BIGPROJECT);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFfindAllTasksAtTaskB() throws Exception {
		var taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		var detectedTasks = processInspector.findAllTasks(taskB, null);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		var detectedTasks = processInspector.findAllTasks(taskC, null);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtNewStart() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var detectedTasks = processInspector.findAllTasks(newStart, UseCase.BIGPROJECT);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, null);

		assertArrayEquals(Arrays.array("Task A"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskB() throws Exception {
		var taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		var detectedTasks = processInspector.findTasksOnPath(taskB, null, null);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskC() throws Exception {
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		var detectedTasks = processInspector.findTasksOnPath(taskC, null, null);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtNewStart() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var detectedTasks = processInspector.findTasksOnPath(newStart, null, null);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathWithProcessFlowOverridesAtStart() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var flowOverrides = new HashMap<String, String>();
		flowOverrides.put("18DC44E096FDFF75-f8", "18DC44E096FDFF75-f12");
		processInspector.setProcessFlowOverrides(flowOverrides);

		var detectedTasks = processInspector.findTasksOnPath(newStart, null, null);

		var expected = Arrays.array("Task C", "Task B");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, "internal");

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtNewStart() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var detectedTasks = processInspector.findTasksOnPath(newStart, null, "internal");

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksOfExternalFlowAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, "external");

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathOfExternalFlowAtNewStart() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var detectedTasks = processInspector.findTasksOnPath(newStart, null, "external");

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathOfMixedFlowAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, "mixed");

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindTasksOnPathOfMixedFlowAtNewStart() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var detectedTasks = processInspector.findTasksOnPath(newStart, null, "mixed");

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldCalculateTotalDurationWithDefault() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processInspector.calculateWorstCaseDuration(start, null);
		assertEquals(0, duration.toHours());
	}

	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processInspector.calculateWorstCaseDuration(start, UseCase.SMALLPROJECT);
		assertEquals(5, duration.toHours());
	}

	@Test
	void shouldCheckCustomInfo() throws Exception {
		var newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		var detectedTasks = processInspector.findTasksOnPath(newStart, null, "internal");
		assertEquals("abc", ((DetectedTask) detectedTasks.get(0)).getCustomInfo());
	}
}

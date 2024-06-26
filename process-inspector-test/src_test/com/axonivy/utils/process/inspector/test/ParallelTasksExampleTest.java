package com.axonivy.utils.process.inspector.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.inspector.internal.AdvancedProcessInspector;
import com.axonivy.utils.process.inspector.model.DetectedElement;
import com.axonivy.utils.process.inspector.model.DetectedTask;
import com.axonivy.utils.process.inspector.model.ElementTask;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class ParallelTasksExampleTest extends FlowExampleTest {
	private static final String PROCESS_NAME = "ParallelTasksExample";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processInspector = new AdvancedProcessInspector();
	}

	@Test
	void shouldFindAllTasksAtStartWithFlowNameNullAndContainDefaultTaskName() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findAllTasks(start, null);

		var names = getTaskNames(detectedTasks);
		assertArrayEquals(Arrays.array("TaskA", "Task1B", "Task2", "Task3B", "Task3A"), names);
	}

	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, null);

		var names = getTaskNames(detectedTasks);
		assertArrayEquals(Arrays.array("TaskA", "Task1B", "Task2", "Task3B", "Task3A"), names);
	}

	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameShortcut() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, "shortcut");

		var names = getTaskNames(detectedTasks);
		assertArrayEquals(Arrays.array("TaskA", "Task1B", "Task2"), names);
	}

	@Test
	void shouldFindOverrideDuration() throws Exception {
		Map<ElementTask, Duration> durationOverride = new HashMap<ElementTask, Duration>();
		durationOverride.put(ElementTask.createGateway("18DD185B60B6E769-f15", "TaskA"), Duration.ofHours(10));
		durationOverride.put(ElementTask.createSingle("18DD185B60B6E769-f7"), Duration.ofHours(11));

		var start = ProcessGraphHelper.findByElementName(process, "start");
		List<DetectedElement> detectedTasks = processInspector.setDurationOverrides(durationOverride)
				.findTasksOnPath(start, null, null);

		DetectedTask taskA = (DetectedTask) findByPid(detectedTasks, "18DD185B60B6E769-f15-TaskA");
		DetectedTask task2 = (DetectedTask) findByPid(detectedTasks, "18DD185B60B6E769-f7");

		assertEquals(taskA.getEstimatedDuration().toHours(), 10);
		assertEquals(task2.getEstimatedDuration().toHours(), 11);
	}

	@Test
	void shouldFindDefaultDuration() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, UseCase.BIGPROJECT, null);

		DetectedTask taskA = (DetectedTask) findByPid(detectedTasks, "18DD185B60B6E769-f15-TaskA");

		assertEquals(taskA.getEstimatedDuration().toHours(), 5);
	}
	
	@Test
	void shouldFindAllTasksStart5() throws Exception {

		var start5 = ProcessGraphHelper.findByElementName(process, "start5");
		var detectedTasks = processInspector.findAllTasks(start5, null);

		var expected = Arrays.array("TaskA5", "TaskC5", "TaskB5", "TaskE5", "TaskD5", "TaskF5", "TaskG5");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}

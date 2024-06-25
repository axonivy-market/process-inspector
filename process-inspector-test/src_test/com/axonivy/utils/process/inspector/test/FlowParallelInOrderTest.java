package com.axonivy.utils.process.inspector.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.inspector.internal.AdvancedProcessInspector;
import com.axonivy.utils.process.inspector.model.DetectedTask;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FlowParallelInOrderTest extends FlowExampleTest {

	private static final String PROCESS_NAME = "FlowParallelInOrder";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processInspector = new AdvancedProcessInspector();
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		List<DetectedTask> detectedTasks = processInspector.findAllTasks(start, null).stream()
				.map(DetectedTask.class::cast).toList();

		var expected = Arrays.array("Task 1A", "Task A", "Task B", "Task 1B", "Task C", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldSetRightStartTimestampForTaskD() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		List<DetectedTask> detectedTasks = processInspector.findTasksOnPath(start, null, null).stream()
				.map(DetectedTask.class::cast).toList();

		DetectedTask taskD = detectedTasks.stream().filter(it -> it.getTaskName().equals("Task D")).findFirst()
				.orElse(null);
		DetectedTask taskB = detectedTasks.stream().filter(it -> it.getTaskName().equals("Task B")).findFirst()
				.orElse(null);
		DetectedTask taskC = detectedTasks.stream().filter(it -> it.getTaskName().equals("Task C")).findFirst()
				.orElse(null);
		Duration maxTimeFromBC = Stream.of(taskB.getTimeUntilEnd(), taskC.getTimeUntilEnd()).max(Duration::compareTo)
				.orElse(null);

		assertEquals(maxTimeFromBC, taskD.getTimeUntilStart());
	}

	@Test
	void shouldFindAllTasksAtStart3() throws Exception {
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var detectedTasks = processInspector.findAllTasks(start3, null);

		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2C3", "Task E3", "Task2B3",
				"Task D3", "Task2A3", "Task C3", "Task F3", "Task K3", "Task G3", "Task3A3", "Task I3");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtStart3() throws Exception {

		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var detectedTasks = processInspector.findTasksOnPath(start3, null, "internal");

		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2B3", "Task D3", "Task2A3",
				"Task C3", "Task K3", "Task2C3", "Task G3", "Task3A3", "Task I3");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldCalculateTotalDurationAtStartWithoutUseCase() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processInspector.calculateWorstCaseDuration(start, null);
		assertEquals(0, duration.toHours());
	}

	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processInspector.calculateWorstCaseDuration(start, UseCase.SMALLPROJECT);
		assertEquals(6, duration.toHours());
	}

	@Test
	void shouldCalculateTotalDurationWithBIGPROJECT() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processInspector.calculateWorstCaseDuration(start, UseCase.BIGPROJECT);
		assertEquals(9, duration.toHours());
	}

	@Test
	void shouldFindTasksOnPathAtTaskBAndTaskC() throws Exception {
		var startB = ProcessGraphHelper.findByElementId(process, "f6");
		var startC = ProcessGraphHelper.findByElementId(process, "f8");

		var detectedTasks = processInspector.findTasksOnPath(List.of(startB, startC), null, null);

		var expected = Arrays.array("Task B", "Task C", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskC3AndTaskD3AndTaskE3() throws Exception {
		var startC3 = ProcessGraphHelper.findByElementId(process, "f51");
		var startD3 = ProcessGraphHelper.findByElementId(process, "f49");
		var startE3 = ProcessGraphHelper.findByElementId(process, "f58");

		var detectedTasks = processInspector.findTasksOnPath(List.of(startC3, startD3, startE3), null, null);
		var expected = Arrays.array("Task E3", "Task C3", "Task F3", "Task D3", "Task3A3", "Task I3");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskA2AndTaskE2AndTaskF2AndTaskG2() throws Exception {
		var startA2 = ProcessGraphHelper.findByElementName(process, "TaskA2");
		var startE2 = ProcessGraphHelper.findByElementName(process, "TaskE2");
		var startF2 = ProcessGraphHelper.findByElementName(process, "TaskF2");
		var startG2 = ProcessGraphHelper.findByElementName(process, "TaskG2");

		var detectedTasks = processInspector.findTasksOnPath(List.of(startA2, startE2, startF2, startG2), null, null);

		var expected = Arrays.array("TaskA2", "Task5A2", "TaskC2", "Task5B2", "TaskD2", "Task1B", "TaskG2", "TaskE2",
				"TaskF2", "Task2A", "TaskI2", "Task4A", "TaskK2", "Task3", "TaskJ2");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskC2AndTaskD2AndTaskE2AndTaskF2AndTaskG2() throws Exception {
		var startC2 = ProcessGraphHelper.findByElementName(process, "TaskC2");
		var startD2 = ProcessGraphHelper.findByElementName(process, "TaskD2");
		var startE2 = ProcessGraphHelper.findByElementName(process, "TaskE2");
		var startF2 = ProcessGraphHelper.findByElementName(process, "TaskF2");
		var startG2 = ProcessGraphHelper.findByElementName(process, "TaskG2");

		var detectedTasks = processInspector.findTasksOnPath(List.of(startC2, startD2, startE2, startF2, startG2), null,
				null);
		var expected = Arrays.array("TaskG2", "TaskE2", "TaskF2", "Task2A", "TaskI2", "Task4A", "TaskK2", "TaskC2",
				"TaskD2", "Task3", "TaskJ2");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskG2AndTaskI2() throws Exception {
		var startG2 = ProcessGraphHelper.findByElementName(process, "TaskG2");
		var startI2 = ProcessGraphHelper.findByElementName(process, "TaskI2");

		var detectedTasks = processInspector.findTasksOnPath(List.of(startG2, startI2), null, null);
		var expected = Arrays.array("TaskG2", "TaskI2", "Task4A", "TaskK2", "Task3", "TaskJ2");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtStart5() throws Exception {
		var start5 = ProcessGraphHelper.findByElementName(process, "start5");
		
		var detectedTasks = processInspector.findAllTasks(start5, null);
		var expected = Arrays.array("TaskA5", "TaskC5", "TaskE5", "TaskF5", "TaskI5", "TaskG5", "TaskH5", "TaskD5", "TaskB5", "TaskJ5");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}
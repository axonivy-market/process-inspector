package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FlowExampleComplexTest extends FlowExampleTest {

	private static final String PROCESS_NAME = "FlowExampleComplex";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new ProcessAnalyzer();
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findAllTasks(start, null);

		var expected = Arrays.array("Task A", "Task B", "Task K", "Task2A", "Task H", "Task2B", "Task G", "Task F",
				"Task C", "Task1A", "Task E", "Task1B", "Task D");
		var taskNames = (getTaskNames(detectedTasks));
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtTaskKAndTaskF() throws Exception {
		var taskK = ProcessGraphHelper.findByElementName(process, "Task K");
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");

		var detectedTasks = processAnalyzer.findAllTasks(List.of(taskK, taskF), null);

		var expected = Arrays.array("Task K", "Task F", "Task2A", "Task H", "Task2B", "Task G");
		var taskNames = (getTaskNames(detectedTasks));

		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtTaskFAndTaskB() throws Exception {
		var taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		var taskF = ProcessGraphHelper.findByElementName(process, "Task F");

		var detectedTasks = processAnalyzer.findAllTasks(List.of(taskF, taskB), null);

		var expected = Arrays.array("Task F", "Task K", "Task2A", "Task H", "Task2B", "Task G", "Task B");
		var taskNames = (getTaskNames(detectedTasks));

		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		var detectedTasks = processAnalyzer.findAllTasks(taskC, null);

		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task2A", "Task H", "Task2B",
				"Task G", "Task K", "Task F");
		var taskNames = (getTaskNames(detectedTasks));

		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskCWithInternal() throws Exception {
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		var detectedTasks = processAnalyzer.findTasksOnPath(taskC, null, "internal");

		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task2A", "Task H", "Task2B",
				"Task G");
		var taskNames = (getTaskNames(detectedTasks));

		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtTaskC() throws Exception {
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		var detectedTasks = processAnalyzer.findTasksOnPath(taskC, null, null);

		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task2A", "Task H", "Task2B",
				"Task G", "Task K");
		var taskNames = (getTaskNames(detectedTasks));

		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findTasksOnPath(start, UseCase.BIGPROJECT, "internal");

		var expected = Arrays.array("Task A", "Task B", "Task2A", "Task H", "Task2B", "Task G");
		var taskNames = (getTaskNames(detectedTasks));
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldCalculateEstimateDuratioUseCaseBIGPROJECTAtTaskBAndTaskC() throws Exception {
		var taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		Duration duration = processAnalyzer.calculateWorstCaseDuration(List.of(taskB, taskC), UseCase.BIGPROJECT);

		assertEquals(Duration.ofHours(15), duration);
	}

	@Test
	void shouldFindTasksOnPathWithProcessFlowOverridesAtTaskC() throws Exception {
		var flowOverrides = new HashMap<String, String>();
		flowOverrides.put("18DF31B990019995-f47", "18DF31B990019995-f28");
		processAnalyzer.setProcessFlowOverrides(flowOverrides);
		var taskC = ProcessGraphHelper.findByElementName(process, "Task C");
		var detectedTasks = processAnalyzer.findTasksOnPath(taskC, null, null);

		var expected = Arrays.array("Task C", "Task1A", "Task E", "Task1B", "Task D", "Task F", "Task K");
		var taskNames = (getTaskNames(detectedTasks));

		assertArrayEquals(expected, taskNames);
	}
}

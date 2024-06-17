package com.axonivy.utils.process.analyzer.test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;
import com.axonivy.utils.process.analyzer.model.DetectedTask;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FlowSubProcessTest extends FlowExampleTest {
	private static final String PROCESS_NAME = "FlowSubprocess";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new ProcessAnalyzer();
	}

	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findTasksOnPath(start, null, null);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findAllTasks(start, null);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindAllTasksAtStart4() throws Exception {
		var start4 = ProcessGraphHelper.findByElementName(process, "start4");
		var detectedTasks = processAnalyzer.findAllTasks(start4, null);

		var expected = Arrays.array("Sub0-TaskA", "Sub00-TaskA", "Sub00-TaskB", "Sub1-TaskA", "Sub2-TaskE", "Sub2-TaskB", "Sub2-TaskC", "Sub3-TaskA", "Sub2-TaskA", "Sub2-TaskD");
		var taskNames = getTaskNames(detectedTasks);
		
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindAllTasksAtStart4OnEndPath() throws Exception {
		var start4 = ProcessGraphHelper.findByElementName(process, "start4");
		var detectedTasks = processAnalyzer.findTasksOnPath(start4, null, "happy");

		var expected = Arrays.array("Sub0-TaskA", "Sub00-TaskA");
		var taskNames = getTaskNames(detectedTasks);
		
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTaskParentNames() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findAllTasks(start, null);
		var parentElementNames = detectedTasks.stream().filter(item -> item.getTaskName().equals("Task A")).findFirst()
				.map(DetectedTask.class::cast).map(DetectedTask::getParentElementNames).orElse(emptyList());

		assertEquals(java.util.Arrays.asList("sub with two levels", "2nd level sub"), parentElementNames);
	}

	@Test
	void shouldFindSubProcessTestAtStart2() throws Exception {
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		var detectedTasks = processAnalyzer.findAllTasks(start2, UseCase.BIGPROJECT);
				
		var expected = Arrays.array("Task sub", "Sub2-TaskA");
		var taskNames = getTaskNames(detectedTasks);		
		assertArrayEquals(expected, taskNames);
		
		var detectedTask = (DetectedTask) detectedTasks.get(0);
		assertEquals("18DE58E0441486DF-f5", detectedTask.getPid());
		assertEquals("Custom info", detectedTask.getCustomInfo());
		assertEquals("FlowSubProcessCall", detectedTask.getElementName());
		assertEquals("Task sub", detectedTask.getTaskName());
		assertEquals(Duration.ofHours(5), detectedTask.getEstimatedDuration());
		assertEquals(Duration.ZERO, detectedTask.getTimeUntilStart());
		assertEquals(Duration.ofHours(5), detectedTask.getTimeUntilEnd());

	}
}

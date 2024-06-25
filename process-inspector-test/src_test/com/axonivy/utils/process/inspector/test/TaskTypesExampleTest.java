package com.axonivy.utils.process.inspector.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.inspector.internal.AdvancedProcessInspector;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TaskTypesExampleTest extends FlowExampleTest {
	private static final String PROCESS_NAME = "TaskTypesExample";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processInspector = new AdvancedProcessInspector();
	}

	@Test
	void shouldFindAllTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findTasksOnPath(start, null, null);

		var names = getTaskNames(detectedTasks);
		assertArrayEquals(Arrays.array("UserTask", "Task", "Tasks-TaskA", "Tasks-TaskB"), names);
	}

	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector	.findAllTasks(start, null);

		var names = getTaskNames(detectedTasks);
		assertArrayEquals(Arrays.array("UserTask", "Task", "Tasks-TaskA", "Tasks-TaskB"), names);
	}

}

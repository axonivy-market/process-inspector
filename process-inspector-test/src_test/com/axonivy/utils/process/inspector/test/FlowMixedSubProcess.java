package com.axonivy.utils.process.inspector.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.inspector.internal.AdvancedProcessInspector;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FlowMixedSubProcess extends FlowExampleTest {
	private static final String PROCESS_NAME = "FlowMixedSubProcess";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processInspector = new AdvancedProcessInspector();
	}

	@Test
	void shouldFindAllTasks() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processInspector.findAllTasks(start, UseCase.BIGPROJECT);

		var expected = Arrays.array("TaskA", "SubC-TaskA", "SubB-TaskB", "SubA-TaskA", "SubA-TaskC", "SubA-TaskB",
				"SubD-TaskB", "SubB-TaskA", "SubB-TaskC", "SubB-TaskD", "SubB-TaskE", "SubD-TaskC", "SubC-TaskB",
				"SubD-TaskD", "SubD-TaskA");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}

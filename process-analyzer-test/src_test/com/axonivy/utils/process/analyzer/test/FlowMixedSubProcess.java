package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;

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
		processAnalyzer = new ProcessAnalyzer();
	}

	@Test
	void shouldFindAllTasks() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findAllTasks(start, UseCase.BIGPROJECT);
		
		var expected = Arrays.array("TaskA", "SubA-TaskA", "SubA-TaskC", "SubA-TaskB", "SubD-TaskB", "SubB-TaskA", "SubD-TaskC", "SubC-TaskA", "SubD-TaskB", "SubD-TaskA");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}

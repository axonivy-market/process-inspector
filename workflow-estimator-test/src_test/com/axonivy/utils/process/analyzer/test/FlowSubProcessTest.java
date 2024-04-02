package com.axonivy.utils.process.analyzer.test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import com.axonivy.utils.process.analyzer.model.DetectedTask;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowSubProcessTest extends FlowExampleTest {
	
	private static BaseElement start;
	private static final String PROCESS_NAME = "FlowSubprocess";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findTasksOnPath(start);
		
		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindAllTasksAtStartWithFlowNameNull() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findAllTasks(start);
		
		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTaskParentNames() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findAllTasks(start);
		var parentElementNames = detectedTasks.stream().filter(item -> item.getTaskName().equals("Task A")).findFirst()
				.map(DetectedTask.class::cast).map(DetectedTask::getParentElementNames)
				.orElse(emptyList());
		
		assertEquals(java.util.Arrays.asList("sub with two levels", "2nd level sub") , parentElementNames);
	}
}

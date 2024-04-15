package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import com.axonivy.utils.process.analyzer.model.DetectedTask;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleBasicTest extends FlowExampleTest {

	private static BaseElement start;
	private static BaseElement newStart;
	private static BaseElement taskB;
	private static BaseElement taskC;
	private static final String PROCESS_NAME = "FlowExampleBasic";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
		newStart = ProcessGraphHelper.findByElementName(process, "NewStart");
		taskB = ProcessGraphHelper.findByElementName(process, "Task B");
		taskC = ProcessGraphHelper.findByElementName(process, "Task C");
	}
	
	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new AdvancedProcessAnalyzer(process);	
	}
	
	@Test
	void shouldFindAllTasksAtStartIncludeAlternative() throws Exception {
		processAnalyzer.enableDescribeAlternativeElements();
		var detectedTasks = processAnalyzer.findAllTasks(start, UseCase.BIGPROJECT);

		var expected = Arrays.array("Task A", "Alter", "int/ext?", "Alter2", "Task C", "Task B");
		assertArrayEquals(expected, getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var detectedTasks = processAnalyzer.findAllTasks(start, UseCase.BIGPROJECT);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFfindAllTasksAtTaskB() throws Exception {
		var detectedTasks = processAnalyzer.findAllTasks(taskB, null);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));	
	}

	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var detectedTasks = processAnalyzer.findAllTasks(taskC, null);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtNewStart() throws Exception {
		var detectedTasks = processAnalyzer.findAllTasks(newStart, UseCase.BIGPROJECT);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(start, null, null);
	
		assertArrayEquals(Arrays.array("Task A"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskB() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(taskB, null, null);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskC() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(taskC, null, null);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtNewStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart, null, null);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithProcessFlowOverridesAtStart() throws Exception {		
		var flowOverrides = new HashMap<String, String>();
		flowOverrides.put("18DC44E096FDFF75-f8", "18DC44E096FDFF75-f12");
		processAnalyzer.setProcessFlowOverrides(flowOverrides);
				
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart, null, null);

		var expected = Arrays.array("Task C",  "Task B");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(start, null, "internal");

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtNewStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart, null, "internal");
		
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindAllTasksOfExternalFlowAtStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(start, null, "external");

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfExternalFlowAtNewStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart, null, "external");

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfMixedFlowAtStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(start, null, "mixed");

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfMixedFlowAtNewStart() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart, null, "mixed");

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldCalculateTotalDurationWithDefault() throws Exception {
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start, null);
		assertEquals(0, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start, UseCase.SMALLPROJECT);
		assertEquals(5, duration.toHours());
	}
	
	@Test
	void shouldCheckCustomInfo() throws Exception {
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart, null, "internal");
		assertEquals("abc", ((DetectedTask)detectedTasks.get(0)).getCustomInfo());
	}
}

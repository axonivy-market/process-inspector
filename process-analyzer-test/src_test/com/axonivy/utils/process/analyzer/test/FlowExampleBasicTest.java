package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.HashMap;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
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
	
	@Test
	void shouldFindAllTasksAtStartIncludeAlternative() throws Exception {
		var processAnalyzer =  new AdvancedProcessAnalyzer(process, null, null);
		processAnalyzer.enableDescribeAlternativeElements();
		var detectedTasks = processAnalyzer.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Alter", "int/ext?", "Task B", "Alter2", "Task C"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task B", "Task C"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFfindAllTasksAtTaskB() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findAllTasks(taskB);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));	
	}

	@Test
	void shouldFindAllTasksAtTaskC() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findAllTasks(taskC);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}

	@Test
	void shouldFindAllTasksAtNewStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findAllTasks(newStart);
	
		assertArrayEquals(Arrays.array("Task B", "Task C"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findTasksOnPath(start);
	
		assertArrayEquals(Arrays.array("Task A"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskB() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findTasksOnPath(taskB);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtTaskC() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findTasksOnPath(taskC);
	
		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithoutFlowNameAtNewStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart);
	
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathWithProcessFlowOverridesAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);		
		var flowOverrides = new HashMap<String, String>();
		flowOverrides.put("18DC44E096FDFF75-f8", "18DC44E096FDFF75-f12");
		processAnalyzer.setProcessFlowOverrides(flowOverrides);
				
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart);

		var expected = Arrays.array("Task C",  "Task B");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksOfInternalFlowAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "internal");
		var detectedTasks = processAnalyzer.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task B", "Task C"), getTaskNames(detectedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "internal");
		var detectedTasks = processAnalyzer.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));		
	}

	@Test
	void shouldFindTasksOnPathOfInternalFlowAtNewStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "internal");
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart);
		
		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindAllTasksOfExternalFlowAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "external");
		var detectedTasks = processAnalyzer.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task C", "Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfExternalFlowAtNewStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "external");
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart);

		assertArrayEquals(Arrays.array("Task C", "Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfMixedFlowAtStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "mixed");
		var detectedTasks = processAnalyzer.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldFindTasksOnPathOfMixedFlowAtNewStart() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "mixed");
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart);

		assertArrayEquals(Arrays.array("Task B"), getTaskNames(detectedTasks));		
	}
	
	@Test
	void shouldCalculateTotalDurationWithDefault() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		Duration duration = processAnalyzer.calculateEstimatedDuration(start);
		assertEquals(15, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, UseCase.SMALLPROJECT, null);
		Duration duration = processAnalyzer.calculateEstimatedDuration(start);
		assertEquals(5, duration.toHours());
	}
	
	@Test
	void shouldCheckCustomInfo() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "internal");
		var detectedTasks = processAnalyzer.findTasksOnPath(newStart);
		assertEquals("abc", ((DetectedTask)detectedTasks.get(0)).getCustomInfo());
	}
}

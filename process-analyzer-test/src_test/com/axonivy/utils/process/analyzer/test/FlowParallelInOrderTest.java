package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import com.axonivy.utils.process.analyzer.model.DetectedTask;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
@SuppressWarnings("restriction")
public class FlowParallelInOrderTest extends FlowExampleTest {
		
	private static final String PROCESS_NAME = "FlowParallelInOrder";
		
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}
	
	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new AdvancedProcessAnalyzer(process);	
	}
	
	@Test
	void shouldFindAllTasksAtStart() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		List<DetectedTask> detectedTasks = processAnalyzer.findAllTasks(start, null).stream().map(DetectedTask.class::cast).toList();

		var expected = Arrays.array("Task 1A", "Task A", "Task B", "Task 1B",  "Task C", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);			
	}
	
	@Test
	void shouldSetRightStartTimestampForTaskD() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		List<DetectedTask> detectedTasks = processAnalyzer.findTasksOnPath(start, null, null).stream().map(DetectedTask.class::cast).toList();
		
		DetectedTask taskD = detectedTasks.stream().filter(it -> it.getTaskName().equals("Task D")).findFirst().orElse(null);
		DetectedTask taskB = detectedTasks.stream().filter(it -> it.getTaskName().equals("Task B")).findFirst().orElse(null);
		DetectedTask taskC = detectedTasks.stream().filter(it -> it.getTaskName().equals("Task C")).findFirst().orElse(null);
		Duration maxTimeFromBC = Stream.of(taskB.getTimeUntilEnd(), taskC.getTimeUntilEnd())
				.max(Duration::compareTo)
				.orElse(null);
		
		assertEquals(maxTimeFromBC, taskD.getTimeUntilStart());	
	}
	
	@Test
	void shouldFindAllTasksAtStart3() throws Exception {		
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var detectedTasks = processAnalyzer.findAllTasks(start3, null);		
		
		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2C3", "Task E3", "Task2B3", "Task D3", "Task2A3",
				"Task C3", "Task F3", "Task K3", "Task G3", "Task3A3", "Task I3");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtStart3() throws Exception {
		
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var detectedTasks = processAnalyzer.findTasksOnPath(start3, null, "internal");		
		
		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2B3", "Task D3", "Task2A3",
				"Task C3", "Task K3", "Task2C3", "Task G3", "Task3A3", "Task I3");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldCalculateTotalDurationAtStartWithoutUseCase() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start, null);
		assertEquals(0, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithSMALPROJECT() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start, UseCase.SMALLPROJECT);
		assertEquals(6, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithBIGPROJECT() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start, UseCase.BIGPROJECT);
		assertEquals(9, duration.toHours());
	}
	
	@Test
	void shouldFindTasksOnPathAtTaskBAndTaskC() throws Exception {
		var startB = ProcessGraphHelper.findByElementId(process, "f6");
		var startC = ProcessGraphHelper.findByElementId(process, "f8");
		
		var detectedTasks = processAnalyzer.findTasksOnPath(List.of(startB, startC), null, null);		
		
		var expected = Arrays.array("Task B", "Task C", "Task D");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtTaskC3AndTaskD3AndTaskE3() throws Exception {
		var startC3 = ProcessGraphHelper.findByElementId(process, "f51");
		var startD3 = ProcessGraphHelper.findByElementId(process, "f49");
		var startE3 = ProcessGraphHelper.findByElementId(process, "f58");
		
		var detectedTasks = processAnalyzer.findTasksOnPath(List.of(startC3, startD3, startE3), null, null);
		var expected = Arrays.array("Task E3", "Task C3", "Task F3", "Task D3", "Task3A3", "Task I3");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
}
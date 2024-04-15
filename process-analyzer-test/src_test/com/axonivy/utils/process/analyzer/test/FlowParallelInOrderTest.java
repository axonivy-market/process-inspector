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
	void shouldFindAllTasksAtTask2() throws Exception {		
		var task2 = ProcessGraphHelper.findByElementId(process, "f17");
		var detectedTasks = processAnalyzer.findAllTasks(task2, null);
		
		var expected = Arrays.array("Task 2B", "Task G", "Task K", "Task M", "Task 2A", "Task F", "Task H", "Task 2C", "Task E", "Task I");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindAllTasksAtStart3() throws Exception {		
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var detectedTasks = processAnalyzer.findAllTasks(start3, null);		
		
		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2B3", "Task D3", "Task2A3", "Task C3", "Task F3", "Task K3", "Task2C3", "Task E3", "Task3A3", "Task I3");
		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldFindTasksOnPathAtStart3() throws Exception {
		
		var start3 = ProcessGraphHelper.findByElementName(process, "start3");
		var detectedTasks = processAnalyzer.findTasksOnPath(start3, null, "internal");		
		
		var expected = Arrays.array("Task1A3", "Task B3", "Task1B3", "Task A3", "Task2B3", "Task D3", "Task2A3", "Task C3", "Task K3", "Task2C3", "Task3A3", "Task I3");
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
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start2, UseCase.SMALLPROJECT);
		assertEquals(10, duration.toHours());
	}
	
	@Test
	void shouldCalculateTotalDurationWithBIGPROJECT() throws Exception {
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		Duration duration = processAnalyzer.calculateWorstCaseDuration(start2, UseCase.BIGPROJECT);
		assertEquals(15, duration.toHours());
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
	void shouldFindTasksOnPathAtTaskFAndTaskEAndTaskG() throws Exception {
		var startF = ProcessGraphHelper.findByElementId(process, "f19");
		var startE = ProcessGraphHelper.findByElementId(process, "f33");
		var startG = ProcessGraphHelper.findByElementId(process, "f20");
		
		var detectedTasks = processAnalyzer.findTasksOnPath(List.of(startF, startE, startG), null, null);
		var expected = Arrays.array("Task G", "Task K", "Task M", "Task F", "Task H", "Task E", "Task I");

		var taskNames = getTaskNames(detectedTasks);
		assertArrayEquals(expected, taskNames);
	}
	
	@Test
	void shouldCalculateTotalDurationWithUseCaseBIGPROJECTAtTaskFAndTaskEAndTaskG() throws Exception {
		var startF = ProcessGraphHelper.findByElementId(process, "f19");
		var startE = ProcessGraphHelper.findByElementId(process, "f33");
		var startG = ProcessGraphHelper.findByElementId(process, "f20");
		
		var duration = processAnalyzer.calculateWorstCaseDuration(List.of(startF, startE, startG), UseCase.BIGPROJECT);		
		
		assertEquals(15, duration.toHours());
	}
}
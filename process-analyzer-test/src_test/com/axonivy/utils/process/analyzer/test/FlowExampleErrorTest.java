package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.internal.ProcessAnalyzer;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FlowExampleErrorTest extends FlowExampleTest {

	private static final String PROCESS_NAME = "FlowExampleError";

	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
	}

	@BeforeEach
	public void setupForEach() {
		processAnalyzer = new ProcessAnalyzer();
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameSuccess() throws Exception {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		var detectedTasks = processAnalyzer.findTasksOnPath(start, null, "success");

		assertEquals(1, detectedTasks.size());
		assertEquals("Task A", getTaskNames(detectedTasks)[0]);
	}
	
	@Test
	void shouldThrowExceptionWhenFindTasksOnPathAtStartWithFlowNameNull()  {
		var start = ProcessGraphHelper.findByElementName(process, "start");
		Exception exception = assertThrows(Exception.class, () -> {
			processAnalyzer.findTasksOnPath(start, null, null);
	    });

	    String expectedMessage = "Not found path";
	    String actualMessage = exception.getMessage();

	    assertEquals(expectedMessage, actualMessage);
	}
	
	@Test
	void shouldThrowExceptionWhenFindTasksOnPathAtStart2WithInternal()  {
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		Exception exception = assertThrows(Exception.class, () -> {
			processAnalyzer.findTasksOnPath(start2, null, "internal");
	    });

	    String expectedMessage = "Have more than one out going with flowname internal";
	    String actualMessage = exception.getMessage();

	    assertEquals(expectedMessage, actualMessage);
	}
	
	@Test
	void shouldThrowExceptionWhenFindTasksOnPathAtStart2WithFlowNameNull()  {
		var start2 = ProcessGraphHelper.findByElementName(process, "start2");
		Exception exception = assertThrows(Exception.class, () -> {
			processAnalyzer.findTasksOnPath(start2, null, null);
	    });

	    String expectedMessage = "Have more than one out going with default path";
	    String actualMessage = exception.getMessage();

	    assertEquals(expectedMessage, actualMessage);
	}
}

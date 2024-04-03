package com.axonivy.utils.process.analyzer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleErrorTest extends FlowExampleTest {

	private static BaseElement start;
	private static final String PROCESS_NAME = "FlowExampleError";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);	
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameSuccess() throws Exception {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, "success");
		var detectedTasks = processAnalyzer.findTasksOnPath(start);

		assertEquals(1, detectedTasks.size());
		assertEquals("Task A", getTaskNames(detectedTasks)[0]);
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull()  {
		var processAnalyzer = new AdvancedProcessAnalyzer(process, null, null);
		
		Exception exception = assertThrows(Exception.class, () -> {
			processAnalyzer.findTasksOnPath(start);
	    });

	    String expectedMessage = "Not found path";
	    String actualMessage = exception.getMessage();

	    assertEquals(expectedMessage, actualMessage);
	}
	
}

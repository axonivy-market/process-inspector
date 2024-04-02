package com.axonivy.utils.estimator.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.AdvancedProcessAnalyzer;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.BaseElement;

@IvyTest
@SuppressWarnings("restriction")
public class FlowExampleLoopTest extends FlowExampleTest {

	private static BaseElement start;
	private static final String PROCESS_NAME = "FlowExampleLoop";
	
	@BeforeAll
	public static void setup() {
		setup(PROCESS_NAME);
		start = ProcessGraphHelper.findByElementName(process, "start");
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new AdvancedProcessAnalyzer(process, null, null);
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);
		
		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindAllTasksStartWithFlowNameNull() throws Exception {
		var workflowEstimator = new AdvancedProcessAnalyzer(process, null, null);
		var estimatedTasks = workflowEstimator.findAllTasks(start);

		assertArrayEquals(Arrays.array("Task A", "Task B"), getTaskNames(estimatedTasks));
	}
	
	@Test
	void shouldFindTasksOnPathAtStartWithFlowNameSuccess() throws Exception {
		var workflowEstimator = new AdvancedProcessAnalyzer(process, null, "success");
		var estimatedTasks = workflowEstimator.findTasksOnPath(start);

		assertArrayEquals(Arrays.array("Task A"), getTaskNames(estimatedTasks));
	}
	
}

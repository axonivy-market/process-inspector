package com.axonivy.utils.estimator.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.process.IProcessManager;
import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@IvyTest
@SuppressWarnings("restriction")
public class WorkflowEstimatorTest {

	private Process process;
	private WorkflowEstimator workflowEstimator;

	@BeforeEach
	void setup() {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		this.process = manager.findProcessByPath("MainTest").getModel();

		// workflowEstimator = new WorkflowEstimator(process, null, "");

	}

	@Test
	void shouldfindAllTasks() {
		System.out.println("--------------------");
		// workflowEstimator.findAllTasks(process.);
	}
}

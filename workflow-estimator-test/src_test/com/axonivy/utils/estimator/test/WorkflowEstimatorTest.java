package com.axonivy.utils.estimator.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.estimator.WorkflowEstimator;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

@IvyTest
public class WorkflowEstimatorTest {

	private IProcessStart processStart;
	private WorkflowEstimator workflowEstimator;
	
	private
	
	@BeforeEach
	void setup() {
		IProcessModelVersion pmv = Ivy.request().getProcessModelVersion();		
		processStart = IWorkflowProcessModelVersion.of(pmv).findProcessStartByUserFriendlyRequestPath("MainTest/start.ivp");			
		//workflowEstimator = new WorkflowEstimator(process, null, "");
		
	}

	@Test
	void shouldfindAllTasks() {
		System.out.println("--------------------");		
		//workflowEstimator.findAllTasks(process.);
	}
}

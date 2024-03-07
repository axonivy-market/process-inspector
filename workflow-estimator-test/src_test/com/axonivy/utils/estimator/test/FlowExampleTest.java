package com.axonivy.utils.estimator.test;

import java.util.List;

import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

public abstract class FlowExampleTest {
	
	protected static Process process;

	protected static void setup(String processName) {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		process = manager.findProcessByPath(processName).getModel();		
	}
	
	protected String[] getTaskNames(List<EstimatedTask> tasks ) {
		return tasks.stream().map(EstimatedTask::getTaskName).toArray(String[]::new);
	}
}

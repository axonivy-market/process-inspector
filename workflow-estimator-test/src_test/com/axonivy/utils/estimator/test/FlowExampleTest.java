package com.axonivy.utils.estimator.test;

import java.util.List;

import com.axonivy.utils.process.analyzer.model.DetectedElement;

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
	
	protected String[] getTaskNames(List<? extends DetectedElement> tasks ) {
		return tasks.stream().map(DetectedElement::getTaskName).toArray(String[]::new);
	}
}

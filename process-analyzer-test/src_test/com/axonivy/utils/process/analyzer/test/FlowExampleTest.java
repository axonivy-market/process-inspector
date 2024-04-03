package com.axonivy.utils.process.analyzer.test;

import java.util.List;

import com.axonivy.utils.process.analyzer.model.DetectedElement;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

public abstract class FlowExampleTest {
	
	protected static Process process;

	protected static void setup(String processName) {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		process = manager.findProcessByPath(processName).getModel();		
	}
	
	protected static Process getProcess(ICase icase) {		
		var processName = icase.getProcessStart().getUserFriendlyRequestPath().split("/")[0];
		IWorkflowProcessModelVersion pmv = icase.getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		var processRdm = manager.findProcessByPath(processName, true).getModel();
		return (Process) processRdm;
	}
	
	protected String[] getTaskNames(List<? extends DetectedElement> tasks ) {
		return tasks.stream().map(DetectedElement::getTaskName).toArray(String[]::new);
	}
}

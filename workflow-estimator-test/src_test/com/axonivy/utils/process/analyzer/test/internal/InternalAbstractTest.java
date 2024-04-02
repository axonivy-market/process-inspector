package com.axonivy.utils.process.analyzer.test.internal;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

@SuppressWarnings("restriction")
abstract class InternalAbstractTest {
	protected Process getProcessByName(String processName) {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		return manager.findProcessByPath(processName).getModel();		
	}
	
}

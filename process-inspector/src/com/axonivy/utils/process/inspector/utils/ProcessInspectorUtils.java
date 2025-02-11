package com.axonivy.utils.process.inspector.utils;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcess;
import ch.ivyteam.ivy.process.rdm.IProcessManager;

public class ProcessInspectorUtils {
	public static List<Process> getAllProcesses() {
		List<Process> processes = IProcessManager.instance().getProjectDataModels().stream()
				.flatMap(project -> project.getProcesses().stream()).map(IProcess::getModel).toList();
		return processes;
	}

	public static Process getProcessByName(String processName) {
		var pmv = Ivy.request().getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		return manager.findProcessByPath(processName).getModel();
	}
}
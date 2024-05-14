package com.axonivy.utils.process.analyzer.helper;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.rdm.IProcessManager;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

public class TaskHelper {

	public static BaseElement getBaseElementOf(ITask task) {
		if (task == null) {
			return null;
		}

		var pid = task.getStart().getProcessElementId();
		IWorkflowProcessModelVersion pmv = task.getProcessModelVersion();
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		Process processRdm = (Process) manager.findProcess(pid.getProcessGuid(), true).getModel();
		BaseElement taskElement = processRdm.search().pid(pid).findOneDeep();
		return taskElement;
	}
}

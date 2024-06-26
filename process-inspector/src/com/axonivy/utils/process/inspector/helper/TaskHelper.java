package com.axonivy.utils.process.inspector.helper;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.value.PID;
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
		Process processRdm = manager.findProcess(pid.getProcessGuid(), true).getModel();
		BaseElement taskElement = processRdm.search().pid(pid).findOneDeep();
		return taskElement;
	}
	
	public static BaseElement getBaseElementByPid(PID pid, IWorkflowProcessModelVersion pmv) {
		String processGuid = pid.getRawPid().split("-")[0];
				
		var manager = IProcessManager.instance().getProjectDataModelFor(pmv);
		Process processRdm = manager.findProcess(processGuid, true).getModel();
		BaseElement taskElement = processRdm.search().pid(pid).findOneDeep();
		return taskElement;
	}	
	
}

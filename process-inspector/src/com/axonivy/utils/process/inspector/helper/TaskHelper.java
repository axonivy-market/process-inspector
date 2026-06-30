package com.axonivy.utils.process.inspector.helper;

import java.util.Objects;

import ch.ivyteam.ivy.process.loader.ProcessLoader;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.value.PID;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.IWorkflowProcessModelVersion;

public class TaskHelper {

	public static BaseElement getBaseElementOf(ITask task) {
		if (task == null) {
			return null;
		}

		var pid = task.getStart().getProcessElementId();
		return getBaseElementByPid(pid, task.getProcessModelVersion());
	}
	
	public static BaseElement getBaseElementByPid(PID pid, IWorkflowProcessModelVersion pmv) {
		var process =	ProcessLoader.of(pmv).loadById(pid.getProcessGuid());
		return process.map(p -> p.search().pid(pid).findOneDeep())
			.filter(Objects::nonNull)
			.orElse(null);
	}	
	
}

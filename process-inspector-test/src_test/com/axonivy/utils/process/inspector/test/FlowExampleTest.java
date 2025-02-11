package com.axonivy.utils.process.inspector.test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.axonivy.utils.process.inspector.ProcessInspector;
import com.axonivy.utils.process.inspector.model.DetectedElement;
import com.axonivy.utils.process.inspector.utils.ProcessInspectorUtils;

import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.workflow.ITask;

public abstract class FlowExampleTest {

	protected static Process process;
	protected ProcessInspector processInspector;

	protected static void setup(String processName) {
		process = ProcessInspectorUtils.getProcessByName(processName);
	}

	protected String[] getTaskNames(List<? extends DetectedElement> tasks) {
		return tasks.stream().map(DetectedElement::getTaskName).toArray(String[]::new);
	}

	protected String[] getElementNames(List<? extends DetectedElement> tasks) {
		return tasks.stream().map(DetectedElement::getElementName).toArray(String[]::new);
	}

	protected DetectedElement findByElementName(List<? extends DetectedElement> tasks, String elementName) {
		return tasks.stream().filter(it -> elementName.equals(it.getElementName())).findFirst().orElse(null);
	}

	protected DetectedElement findByPid(List<DetectedElement> tasks, String pid) {
		return tasks.stream().filter(it -> pid.equals(it.getPid())).findFirst().orElse(null);
	}

	protected ITask findTaskByElementName(List<ITask> tasks, String elementName) {
		return tasks.stream().filter(it -> elementName.equals(it.getName())).findFirst().orElse(null);
	}

	protected LocalDateTime toLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
}

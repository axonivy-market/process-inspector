package com.axonivy.utils.estimator.demo.model;

import java.util.List;

import com.axonivy.utils.estimator.demo.constant.FindType;
import com.axonivy.utils.estimator.model.EstimatedTask;

import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.TaskModifier;

public class Estimator {
	private Process process;
	private TaskModifier from;
	private FindType findType;
	private List<EstimatedTask> tasks;

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public TaskModifier getFrom() {
		return from;
	}

	public void setFrom(TaskModifier from) {
		this.from = from;
	}

	public FindType getFindType() {
		return findType;
	}

	public void setFindType(FindType findType) {
		this.findType = findType;
	}

	public List<EstimatedTask> getTasks() {
		return tasks;
	}

	public void setTasks(List<EstimatedTask> tasks) {
		this.tasks = tasks;
	}
}

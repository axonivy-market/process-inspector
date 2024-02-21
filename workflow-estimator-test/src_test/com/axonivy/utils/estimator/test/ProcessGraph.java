package com.axonivy.utils.estimator.test;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;

@SuppressWarnings("restriction")
public class ProcessGraph {
	public final Process process;

	public ProcessGraph(Process process) {
		this.process = process;
	}

	public RequestStart findStart() {
		return process.search().type(RequestStart.class).findOne();
	}
	
	public SingleTaskCreator findByTaskName(String taskName) {
		
		return null;
	}
}

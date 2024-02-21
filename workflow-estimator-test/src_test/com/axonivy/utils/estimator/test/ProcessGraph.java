package com.axonivy.utils.estimator.test;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
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

	public BaseElement findByElementName(String name) {
		return process.getElements().stream()
				.filter(el -> el.getName().equals(name))
				.findFirst()
				.orElse(null);
	}
}

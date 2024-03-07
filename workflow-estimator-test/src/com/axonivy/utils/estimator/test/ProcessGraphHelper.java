package com.axonivy.utils.estimator.test;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;

@SuppressWarnings("restriction")
public class ProcessGraphHelper {
	
	public static RequestStart findStart(Process process) {
		return process.search().type(RequestStart.class).findOne();
	}

	public static BaseElement findByElementName(Process process, String name) {
		return process.getElements().stream()
				.filter(el -> el.getName().equals(name))
				.findFirst()
				.orElse(null);
	}
	
	public static BaseElement findByTaskName(Process process, String name) {
		return process.getElements().stream()
				.filter(el -> {return el instanceof SingleTaskCreator;})
				.filter(el ->((SingleTaskCreator) el).getTaskConfig().getName().getRawMacro().equals(name))
				.findFirst()
				.orElse(null);
	}
}

package com.axonivy.utils.process.analyzer.test;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.stream.Collectors;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.EmbeddedProcess;
import ch.ivyteam.ivy.process.model.Process;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.SingleTaskCreator;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;

public class ProcessGraphHelper {

	public static RequestStart findStart(Process process) {
		return process.search().type(RequestStart.class).findOne();
	}

	public static BaseElement findByElementId(Process process, String filedId) {
		return getElementOfProcess(process).stream().filter(el -> el.getPid().getFieldId().equals(filedId)).findFirst()
				.orElse(null);
	}

	public static BaseElement findByElementName(Process process, String name) {
		return getElementOfProcess(process).stream().filter(el -> el.getName().equals(name)).findFirst().orElse(null);
	}

	public static BaseElement findByTaskName(Process process, String name) {
		return getElementOfProcess(process).stream().filter(el -> {
			return el instanceof SingleTaskCreator;
		}).filter(el -> ((SingleTaskCreator) el).getTaskConfig().getName().getRawMacro().equals(name)).findFirst()
				.orElse(null);
	}

	private static List<BaseElement> getElementOfProcess(Process process) {
		var processElements = process.getProcessElements();
		var childElments = getElementOfProcesses(processElements);
		var elements = process.getElements();
		elements.addAll(childElments);

		return elements;
	}

	private static List<BaseElement> getElementOfProcesses(List<ProcessElement> processElements) {
		if (processElements.isEmpty()) {
			return emptyList();
		}
		var embeddedProcess = processElements.stream().filter(it -> it instanceof EmbeddedProcessElement == true)
				.map(EmbeddedProcessElement.class::cast).map(it -> it.getEmbeddedProcess())
				.collect(Collectors.toList());

		var elememets = embeddedProcess.stream().map(EmbeddedProcess::getElements).flatMap(List::stream)
				.collect(Collectors.toList());

		var childProcessElements = embeddedProcess.stream().map(EmbeddedProcess::getProcessElements)
				.flatMap(List::stream).collect(Collectors.toList());

		var childElememts = getElementOfProcesses(childProcessElements);

		elememets.addAll(childElememts);

		return elememets;
	}
}

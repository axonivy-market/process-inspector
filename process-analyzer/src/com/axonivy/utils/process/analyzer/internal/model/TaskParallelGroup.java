package com.axonivy.utils.process.analyzer.internal.model;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.value.PID;

@SuppressWarnings("restriction")
public class TaskParallelGroup implements ProcessElement {
	private BaseElement element;
	private Map<SequenceFlow, List<ProcessElement>> internalPaths;

	public TaskParallelGroup(BaseElement element) {
		this.element = element;
	}

	public Map<SequenceFlow, List<ProcessElement>> getInternalPaths() {
		return internalPaths;
	}

	public void setInternalPaths(Map<SequenceFlow, List<ProcessElement>> internalPaths) {
		this.internalPaths = internalPaths;
	}

	@Override
	public int hashCode() {
		if (element != null) {
			return element.hashCode();
		}
		return 0;
	}

	@Override
	public String toString() {
		if (element != null) {
			return element.toString();
		}
		return EMPTY;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TaskParallelGroup) {
			return ((TaskParallelGroup) obj).element.equals(element);
		}
		return false;
	}

	@Override
	public PID getPid() {
		return element.getPid();
	}

	@Override
	public BaseElement getElement() {
		return element;
	}
}

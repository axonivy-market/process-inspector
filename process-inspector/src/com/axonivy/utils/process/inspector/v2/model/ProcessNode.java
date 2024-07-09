package com.axonivy.utils.process.inspector.v2.model;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.NodeElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;

public class ProcessNode {
	NodeElement element;

	private Map<SequenceFlow, Estimated> inComings;
	private Map<SequenceFlow, Estimated> outGoings;

	public ProcessNode(NodeElement element) {
		this.element = element;
		this.inComings = new LinkedHashMap<>();
		this.outGoings = new LinkedHashMap<>();
	}

	public NodeElement getElement() {
		return element;
	}

	public void setElement(NodeElement element) {
		this.element = element;
	}

	public Map<SequenceFlow, Estimated> getInComings() {
		return inComings;
	}

	public Map<SequenceFlow, Estimated> getOutGoings() {
		return outGoings;
	}
}

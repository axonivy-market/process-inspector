package com.axonivy.utils.process.analyzer.internal.model;

import java.util.ArrayList;
import java.util.List;

public class AnalysisPath {
	private List<ProcessElement> elements;

	public AnalysisPath() {
		this.elements = new ArrayList<>();
	}

	public AnalysisPath(List<ProcessElement> elements) {
		this.elements = elements;
	}

	public List<ProcessElement> getElements() {
		return elements;
	}

	public void setElements(List<ProcessElement> elements) {
		this.elements = elements;
	}
}

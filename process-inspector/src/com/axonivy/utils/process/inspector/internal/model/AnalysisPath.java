package com.axonivy.utils.process.inspector.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnalysisPath {
	private List<ProcessElement> elements;

	public AnalysisPath() {
		this.elements = new ArrayList<>();
	}

	public AnalysisPath(ProcessElement element) {
		this.elements = List.of(element);
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

	@Override
	public int hashCode() {
		return Objects.hash(this.elements);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof AnalysisPath)) {
			return false;
		}
		AnalysisPath path = (AnalysisPath) other;
		return Objects.equals(path.elements, this.elements);
	}

	@Override
	public String toString() {
		return Objects.toString(elements);
	}
}

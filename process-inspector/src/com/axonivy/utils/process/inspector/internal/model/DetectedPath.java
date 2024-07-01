package com.axonivy.utils.process.inspector.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.axonivy.utils.process.inspector.model.DetectedElement;

public class DetectedPath {
	private List<DetectedElement> elements;

	public DetectedPath() {
		this.elements = new ArrayList<>();
	}
	
	public DetectedPath(DetectedElement element) {
		this.elements = List.of(element);
	}
	
	public DetectedPath(List<DetectedElement> elements) {
		this.elements = elements;
	}
	
	public List<DetectedElement> getElements() {
		return this.elements;
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
		if (!(other instanceof DetectedPath)) {
			return false;
		}
		DetectedPath path = (DetectedPath) other;
		return Objects.equals(path.elements, this.elements);
	}

	@Override
	public String toString() {
		return Objects.toString(elements);
	}
}

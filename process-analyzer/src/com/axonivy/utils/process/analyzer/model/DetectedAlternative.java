package com.axonivy.utils.process.analyzer.model;

import java.util.List;

public class DetectedAlternative extends DetectedElement {
	private List<DetectedElement> options;

	public List<DetectedElement> getOptions() {
		return options;
	}

	public void setOptions(List<DetectedElement> options) {
		this.options = options;
	}
}

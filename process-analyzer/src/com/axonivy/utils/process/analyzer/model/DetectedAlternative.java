package com.axonivy.utils.process.analyzer.model;

import java.util.List;

public class DetectedAlternative extends DetectedElement {
	private List<DetectedElement> options;
	
	public DetectedAlternative(String pid, String elementName, List<DetectedElement> options) {
		super(pid, elementName);
		this.options = options;
	}
	
	public List<DetectedElement> getOptions() {
		return options;
	}
}

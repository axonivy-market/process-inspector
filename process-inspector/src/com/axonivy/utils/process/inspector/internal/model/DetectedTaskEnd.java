package com.axonivy.utils.process.inspector.internal.model;

import java.time.Duration;

import com.axonivy.utils.process.inspector.model.DetectedElement;

public class DetectedTaskEnd extends DetectedElement {

	private Duration timeUntilStart;
	private Duration timeUntilEnd;

	public DetectedTaskEnd(String pid, String elementName, Duration timeUntilStart) {
		super(pid, elementName);
		this.timeUntilStart = this.timeUntilEnd = timeUntilStart;
	}
}

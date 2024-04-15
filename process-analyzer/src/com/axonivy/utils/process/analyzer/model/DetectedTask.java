package com.axonivy.utils.process.analyzer.model;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class DetectedTask extends DetectedElement {

	private Duration estimatedDuration;
	/**
	 * Names of parent process elements in the order they appeared.
	 * In case the task is not inside of a sub-process element, list will be empty.
	 */
	private List<String> parentElementNames;
	
	private Duration timeUntilStart;
	
	private Duration timeUntilEnd;
	
	/**
	 * Custom string which can be set on the task element
	 */
	private String customInfo;

	public Duration getEstimatedDuration() {
		return estimatedDuration;
	}

	public void setEstimatedDuration(Duration estimatedDuration) {
		this.estimatedDuration = estimatedDuration;
	}

	public List<String> getParentElementNames() {
		return parentElementNames;
	}

	public String getDisplayParentElementNames() {
		return parentElementNames.stream().collect(Collectors.joining(", "));
	}

	public void setParentElementNames(List<String> parentElementNames) {
		this.parentElementNames = parentElementNames;
	}
	
	public Duration getTimeUntilStart() {
		return timeUntilStart;
	}

	public void setTimeUntilStart(Duration timeUntilStart) {
		this.timeUntilStart = timeUntilStart;
	}

	public Duration getTimeUntilEnd() {
		return timeUntilEnd;
	}

	public void setTimeUntilEnd(Duration timeUntilEnd) {
		this.timeUntilEnd = timeUntilEnd;
	}

	public String getCustomInfo() {
		return customInfo;
	}
	

	public void setCustomInfo(String customInfo) {
		this.customInfo = customInfo;
	}
}
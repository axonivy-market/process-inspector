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
	
	public DetectedTask(String pid, String taskName, String elementName, Duration duration, List<String> parentElementNames, Duration timeUntilStart, Duration timeUntilEnd, String customInfo) {
		super(pid, taskName, elementName);
		this.estimatedDuration = duration;
		this.parentElementNames = parentElementNames;
		this.timeUntilStart = timeUntilStart;
		this.timeUntilEnd = timeUntilEnd;
		this.customInfo = customInfo;
	}

	public Duration getEstimatedDuration() {
		return estimatedDuration;
	}

	public List<String> getParentElementNames() {
		return parentElementNames;
	}

	public String getDisplayParentElementNames() {
		return parentElementNames.stream().collect(Collectors.joining(", "));
	}
	
	public Duration getTimeUntilStart() {
		return timeUntilStart;
	}

	public Duration getTimeUntilEnd() {
		return timeUntilEnd;
	}

	public String getCustomInfo() {
		return customInfo;
	}
}
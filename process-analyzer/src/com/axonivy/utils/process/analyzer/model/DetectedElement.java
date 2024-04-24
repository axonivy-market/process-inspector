package com.axonivy.utils.process.analyzer.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DetectedElement {
	private String pid;
	private String taskName;
	private String elementName;
	
	public DetectedElement(String pid, String elementName) {
		this.pid = pid;
		this.elementName = elementName;
	}
	
	public DetectedElement(String pid, String taskName, String elementName) {
		this(pid, elementName);
		this.taskName = taskName;
	}

	public String getPid() {
		return pid;
	}

	public String getTaskName() {
		return taskName;
	}

	public String getElementName() {
		return elementName;
	}
	
	@Override
	public String toString() {
		return Stream.of(getPid(), getTaskName()).collect(Collectors.joining("-"));
	}
}
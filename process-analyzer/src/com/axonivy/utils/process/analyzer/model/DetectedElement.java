package com.axonivy.utils.process.analyzer.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DetectedElement {
	private String pid;
	private String taskName;
	private String elementName;
	
	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	public String getTaskName() {
		return taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public String getElementName() {
		return elementName;
	}
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}
	
	public String getShortPid() {
		int index = pid.indexOf("-");
		return pid.substring(index + 1);
	}
	
	@Override
	public String toString() {
		return Stream.of(getPid(), getTaskName()).collect(Collectors.joining("-"));
	}
}
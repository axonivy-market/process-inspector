package com.axonivy.utils.estimator.model;

public abstract class EstimatedElement {
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
}

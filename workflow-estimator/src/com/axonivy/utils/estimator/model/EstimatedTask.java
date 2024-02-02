package com.axonivy.utils.estimator.model;

import java.util.Date;

public class EstimatedTask {
	private String pid;
	private String taskName;
	private Date estimatedStartTimestamp;

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

	public Date getEstimatedStartTimestamp() {
		return estimatedStartTimestamp;
	}

	public void setEstimatedStartTimestamp(Date estimatedStartTimestamp) {
		this.estimatedStartTimestamp = estimatedStartTimestamp;
	}
}

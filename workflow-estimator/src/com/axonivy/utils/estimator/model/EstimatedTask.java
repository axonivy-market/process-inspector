package com.axonivy.utils.estimator.model;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.time.DateUtils;

public class EstimatedTask {
	
	private String pid;
	private String taskName;
	private Duration estimatedDuration;
	private List<String> parentElementNames;
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

	public Duration getEstimatedDuration() {
		return estimatedDuration;
	}

	public void setEstimatedDuration(Duration estimatedDuration) {
		this.estimatedDuration = estimatedDuration;
	}

	public List<String> getParentElementNames() {
		return parentElementNames;
	}

	public void setParentElementNames(List<String> parentElementNames) {
		this.parentElementNames = parentElementNames;
	}
	
	public Date calculateEstimatedEndTimestamp() {
		int durationInSecond = Optional.ofNullable(this.estimatedDuration)
				.map(Duration::getSeconds)
				.map(se -> Long.valueOf(se).intValue())
				.orElse(0);
		
		return DateUtils.addSeconds(this.estimatedStartTimestamp, durationInSecond);
	}
}

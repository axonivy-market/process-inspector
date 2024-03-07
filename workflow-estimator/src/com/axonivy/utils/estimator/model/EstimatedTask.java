package com.axonivy.utils.estimator.model;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.DateUtils;

public class EstimatedTask {
	
	private String pid;
	private String taskName;
	private String elementName;
	private Duration estimatedDuration;
	/**
	 * Names of parent process elements in the order they appeared.
	 * In case the task is not inside of a sub-process element, list will be empty.
	 */
	private List<String> parentElementNames;
	private Date estimatedStartTimestamp;
	/**
	 * Custom string which can be set on the task element
	 */
	private String customInfo;

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
	
	public String getCustomInfo() {
		return customInfo;
	}

	public void setCustomInfo(String customInfo) {
		this.customInfo = customInfo;
	}
	
	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public Date calculateEstimatedEndTimestamp() {
		int durationInSecond = Optional.ofNullable(this.estimatedDuration)
				.map(Duration::getSeconds)
				.map(se -> Long.valueOf(se).intValue())
				.orElse(0);
		
		return DateUtils.addSeconds(this.estimatedStartTimestamp, durationInSecond);
	}

	@Override
	public String toString() {
		return Stream.of(pid, taskName).collect(Collectors.joining("-"));
	}
}

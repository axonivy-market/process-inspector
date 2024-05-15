package com.axonivy.utils.process.analyzer.model;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ElementTask(String pid, String taskId) {

	public static ElementTask createSingle(String pid) {
		return new ElementTask(pid, null);
	}

	public static ElementTask createGateway(String pid, String taskId) {
		return new ElementTask(pid, taskId);
	}

	public String getId() {
		return Stream.of(pid, taskId).filter(Objects::nonNull).collect(Collectors.joining("-"));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof ElementTask)) {
			return false;
		}
		ElementTask task = (ElementTask) other;
		return Objects.equals(task.pid, this.pid) && Objects.equals(task.taskId, this.taskId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.pid, this.taskId);
	}

	@Override
	public String toString() {
		return pid + "-" + taskId;
	}
}

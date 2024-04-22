package com.axonivy.utils.process.analyzer.model;

public record ElementTask(String pid, String taskId) {
	
	public ElementTask(String pid) {
		this(pid, null);
	}
	
	public String getId() {
		if(taskId != null) {
			return pid + "-" + taskId;
		}
		return pid;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ElementTask) {
			return ((ElementTask)obj).pid.equals(this.pid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.pid.hashCode();
	}
	
	@Override
	public String toString() {
		return pid + "-" + taskId;
	}
}

package com.axonivy.utils.process.inspector.demo.constant;

public enum FindType {
	ALL_TASK("Find All Task"), TASK_ON_PATH("Find Task On Path");

	private final String displayName;

	private FindType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return this.displayName;
	}
}

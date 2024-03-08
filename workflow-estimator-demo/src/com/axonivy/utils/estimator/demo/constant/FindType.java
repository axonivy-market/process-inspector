package com.axonivy.utils.estimator.demo.constant;

public enum FindType {
	ALL_TASK("Find All Task"), TASK_ON_PATH("Find Task On Path");

	private final String findType;

	private FindType(String findType) {
		this.findType = findType;
	}
	public String getFindType() {
		return this.findType;
	}
}

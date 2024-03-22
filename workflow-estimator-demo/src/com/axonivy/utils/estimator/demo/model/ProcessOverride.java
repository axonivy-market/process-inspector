package com.axonivy.utils.estimator.demo.model;

public class ProcessOverride {
	
	private String alternative;
	
	private String target;
	
	public ProcessOverride(String alternative, String target) {
		this.alternative = alternative;
		this.target = target;
	}

	public String getAlternative() {
		return alternative;
	}

	public void setAlternative(String alternative) {
		this.alternative = alternative;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	public String getDisplayName() {
		return this.alternative + " --> " + this.target;
	}

}

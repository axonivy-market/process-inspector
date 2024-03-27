package com.axonivy.utils.estimator.model;

import java.util.List;

public class EstimatedAlternative extends EstimatedElement {
	private List<EstimatedElement> options;

	public List<EstimatedElement> getOptions() {
		return options;
	}

	public void setOptions(List<EstimatedElement> options) {
		this.options = options;
	}
}

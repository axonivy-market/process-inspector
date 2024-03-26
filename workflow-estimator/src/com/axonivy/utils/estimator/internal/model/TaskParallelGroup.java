package com.axonivy.utils.estimator.internal.model;

import java.util.List;
import java.util.Map;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;

@SuppressWarnings("restriction")
public class TaskParallelGroup extends CommonElement {

	private Map<SequenceFlow, List<CommonElement>> internalPaths;

	public TaskParallelGroup(BaseElement element) {
		super(element);
	}

	public Map<SequenceFlow, List<CommonElement>> getInternalPaths() {
		return internalPaths;
	}


	public void setInternalPaths(Map<SequenceFlow, List<CommonElement>> internalPaths) {
		this.internalPaths = internalPaths;
	}


	@Override
	public boolean equals(Object obj) {	
		return super.equals(obj);
	}
}

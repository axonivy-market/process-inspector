package com.axonivy.utils.process.analyzer.internal.model;

import java.util.List;
import java.util.Map;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.connector.SequenceFlow;
import ch.ivyteam.ivy.process.model.value.PID;

@SuppressWarnings("restriction")
public class TaskParallelGroup implements ProcessElement {
	private BaseElement element;
	private Map<SequenceFlow, List<ProcessElement>> internalPaths;

	public TaskParallelGroup(BaseElement element) {
		this.element = element;	
	}

	public Map<SequenceFlow, List<ProcessElement>> getInternalPaths() {
		return internalPaths;
	}

	public void setInternalPaths(Map<SequenceFlow, List<ProcessElement>> internalPaths) {
		this.internalPaths = internalPaths;
	}
	@Override
	public int hashCode() {		
		return element.hashCode();
	}
	
	@Override
	public String toString() {
		return element.toString();
	}
	
	@Override
	public boolean equals(Object obj) {	
		if(obj instanceof TaskParallelGroup) {
			return ((TaskParallelGroup)obj).element.equals(element);
		}
		return false;
	}

	@Override
	public PID getPid() {		
		return element.getPid();
	}

	@Override
	public BaseElement getElement() {		
		return element;
	}
}

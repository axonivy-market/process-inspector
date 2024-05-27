package com.axonivy.utils.process.analyzer.internal.model;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;
import java.util.Objects;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.value.PID;

public class SubProcessGroup implements ProcessElement {
	private BaseElement element;
	List<AnalysisPath> internalPaths;

	public SubProcessGroup(BaseElement element) {
		this.element = element;
	}

	public SubProcessGroup(BaseElement element, List<AnalysisPath> internalPaths) {
		this(element);
		this.internalPaths = internalPaths;
	}
	
	public List<AnalysisPath> getInternalPaths() {
		return internalPaths;
	}

	public void setInternalPaths(List<AnalysisPath> internalPaths) {
		this.internalPaths = internalPaths;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.element, this.internalPaths);
	}

	@Override
	public String toString() {
		return String.format("%s : %s ", Objects.toString(element, EMPTY), Objects.toString(internalPaths, EMPTY));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof SubProcessGroup)) {
			return false;
		}
		SubProcessGroup task = (SubProcessGroup) other;
		return Objects.equals(task.element, this.element) && Objects.equals(task.internalPaths, this.internalPaths);
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

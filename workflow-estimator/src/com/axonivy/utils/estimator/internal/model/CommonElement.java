package com.axonivy.utils.estimator.internal.model;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.value.PID;

@SuppressWarnings("restriction")
public class CommonElement {
	private BaseElement element;

	public CommonElement(BaseElement element) {
		this.element = element;
	}

	public BaseElement getElement() {
		return element;
	}

	public void setElement(BaseElement element) {
		this.element = element;
	}

	public PID getPid() {
		return element.getPid();
	}
	
	@Override
	public boolean equals(Object obj) {	
		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		return element.toString();
	}
}

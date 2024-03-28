package com.axonivy.utils.estimator.internal.model;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.value.PID;

@SuppressWarnings("restriction")
public interface ProcessElement{	
	public PID getPid();
	public BaseElement getElement();
	
}

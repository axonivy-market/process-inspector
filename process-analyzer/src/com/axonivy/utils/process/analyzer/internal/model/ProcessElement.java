package com.axonivy.utils.process.analyzer.internal.model;

import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.process.model.value.PID;

public interface ProcessElement {
	public PID getPid();

	public BaseElement getElement();
}

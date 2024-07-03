package com.axonivy.utils.process.inspector.internal.model;

import java.time.Duration;

import com.axonivy.utils.process.inspector.model.DetectedElement;

public class DetectedEmbeddedEnd extends DetectedElement{
	private String connectedOuterSequenceFlowPid;
	private Duration timeUntilStart;
	private Duration timeUntilEnd;
	
	public DetectedEmbeddedEnd(String pid,  String elementName, String connectedOuterSequenceFlowPid, Duration timeUntilStart) {
		super(pid, elementName);
		this.connectedOuterSequenceFlowPid = connectedOuterSequenceFlowPid;
		this.timeUntilStart = this.timeUntilEnd = timeUntilStart;
	}

	public String getConnectedOuterSequenceFlowPid() {
		return connectedOuterSequenceFlowPid;
	}

	public void setConnectedOuterSequenceFlowPid(String connectedOuterSequenceFlowPid) {
		this.connectedOuterSequenceFlowPid = connectedOuterSequenceFlowPid;
	}

	public Duration getTimeUntilStart() {
		return timeUntilStart;
	}

	public Duration getTimeUntilEnd() {
		return timeUntilEnd;
	}
}

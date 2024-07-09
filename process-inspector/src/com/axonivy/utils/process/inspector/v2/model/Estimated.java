package com.axonivy.utils.process.inspector.v2.model;

import java.time.Duration;

public class Estimated {
	private Duration duration;
	private int version;
	
	public Estimated(Duration duration, int version) {
		this.duration = duration;
		this.version = version;
	}

	public Duration getDuration() {
		return duration;
	}

	public int getVersion() {
		return version;
	}
}

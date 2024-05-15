package com.axonivy.utils.process.analyzer.demo.helper;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class DateTimeHelper {
	private static final String HOUR_MINUTES_FORMAT = "H'h' m'm'";

	public static String getDisplayDuration(Duration duration) {
		if (duration == null) {
			return StringUtils.EMPTY;
		}
		String format = duration.isNegative() ? "- (%s)" : "%s";
		return DurationFormatUtils.formatDuration(duration.abs().toMillis(), String.format(format, HOUR_MINUTES_FORMAT),
				false);
	}
}

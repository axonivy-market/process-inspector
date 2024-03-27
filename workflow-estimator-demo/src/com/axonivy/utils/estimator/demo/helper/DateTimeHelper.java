package com.axonivy.utils.estimator.demo.helper;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

public class DateTimeHelper {

	public static String getDisplayDuration(Duration duration) {
		if(duration.isZero()) {
			return StringUtils.EMPTY;
		}
		return  DurationFormatUtils.formatDuration(duration.toMillis(), "H'h' m'm'", false);
	}
}

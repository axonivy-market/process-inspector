package com.axonivy.utils.process.inspector.helper;

import java.time.Duration;
import java.util.Date;

import ch.ivyteam.ivy.application.calendar.IBusinessCalendar;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.BusinessDuration;
import ch.ivyteam.ivy.scripting.objects.DateTime;

public class DateTimeHelper {
	private static final IBusinessCalendar GERMANY_CALENDAR = Ivy.cal().get("Germany");

	public static Date getBusinessStartTimestamp(Date startJavaDateTime) {
		DateTime startDateTime = new DateTime(startJavaDateTime);
		DateTime result = GERMANY_CALENDAR.getBusinessTimeIn(startDateTime, new BusinessDuration(0));
		return result.toJavaDate();
	}

	public static Duration getBusinessDuration(Date startJavaDateTime, Date endJavaDateTime) {
		DateTime startDateTime = new DateTime(startJavaDateTime);
		DateTime endDateTime = new DateTime(endJavaDateTime);

		BusinessDuration duration = GERMANY_CALENDAR.getBusinessDuration(startDateTime, endDateTime);
		return Duration.ofSeconds(duration.toNumber());
	}
}

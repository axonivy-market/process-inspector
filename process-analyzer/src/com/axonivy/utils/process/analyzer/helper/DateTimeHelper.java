package com.axonivy.utils.process.analyzer.helper;

import java.time.Duration;
import java.util.Date;

import ch.ivyteam.ivy.application.calendar.IBusinessCalendar;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.BusinessDuration;

public class DateTimeHelper {
	private static final IBusinessCalendar GERMANY_CALENDAR = Ivy.cal().get("Germany");

	public static Date getBusinessStartTimestamp(Date startJavaDateTime) {
		ch.ivyteam.ivy.scripting.objects.DateTime startDateTime = new ch.ivyteam.ivy.scripting.objects.DateTime(startJavaDateTime);
		ch.ivyteam.ivy.scripting.objects.DateTime result = GERMANY_CALENDAR.getBusinessTimeIn(startDateTime, new BusinessDuration(0));
		return result.toJavaDate();
	}
	
	public static Duration getBusinessDuration(Date startJavaDateTime, Date endJavaDateTime) {		
		ch.ivyteam.ivy.scripting.objects.DateTime startDateTime = new ch.ivyteam.ivy.scripting.objects.DateTime(startJavaDateTime);
		ch.ivyteam.ivy.scripting.objects.DateTime endDateTime = new ch.ivyteam.ivy.scripting.objects.DateTime(endJavaDateTime);
		
		BusinessDuration duration = GERMANY_CALENDAR.getBusinessDuration(startDateTime, endDateTime);
		return Duration.ofSeconds(duration.toNumber());
	} 
}

package com.axonivy.utils.process.analyzer.test.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.process.analyzer.helper.DateTimeHelper;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class DateTimeHelperTest {
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	@Test
	void shouldGetRightBusinessTimeWithStartTimeAtWeekend() {
		Date date = createDate("2024-04-13 10:30:00");
		
		Date businessTime = DateTimeHelper.getBusinessStartTimestamp(date);
		
		assertEquals("2024-04-15 08:00:00", convertDateString(businessTime));		
	}
	
	@Test
	void shouldGetRightBusinessTimeWithStartTimeAtBusinessTime() {
		Date date = createDate("2024-04-16 15:30:00");
		
		Date businessTime = DateTimeHelper.getBusinessStartTimestamp(date);
		
		assertEquals("2024-04-16 15:30:00", convertDateString(businessTime));		
	}
	
	@Test
	void shouldGetRightBusinessTimeWithStartTimeAtNotBusinessTime() {
		Date date = createDate("2024-04-15 05:30:00");
		
		Date businessTime = DateTimeHelper.getBusinessStartTimestamp(date);
		
		assertEquals("2024-04-15 08:00:00", convertDateString(businessTime));		
	}
	
	
	@Test
	void shouldGetDurationBetweenStartAtWeekendAndEndAtBusinessTime()  {
		Date start = createDate("2024-04-13 10:30:00");
		Date end = createDate("2024-04-15 10:00:00");
		Duration duration = DateTimeHelper.getBusinessDuration(start, end);		
		assertEquals(2, duration.toHours());		
	}
	
	@Test
	void shouldGetDurationBetweenStartAtNoBusinessTimeAndEndAtBusinessTime()  {
		Date start = createDate("2024-04-16 05:30:00");
		Date end = createDate("2024-04-16 10:00:00");
		Duration duration = DateTimeHelper.getBusinessDuration(start, end);		
		assertEquals(2, duration.toHours());		
	}
	
	@Test
	void shouldGetDurationBetweenStartAndEndAtWeekend()  {
		Date start = createDate("2024-04-13 05:30:00");
		Date end = createDate("2024-04-14 10:00:00");
		Duration duration = DateTimeHelper.getBusinessDuration(start, end);		
		assertEquals(0, duration.toHours());		
	}
	
	@Test
	void shouldGetDurationBetweenStartAtPreoisWeekAndEndAtWeek()  {
		//Friday 
		Date start = createDate("2024-04-12 15:00:00");
		//Next Monday
		Date end = createDate("2024-04-15 10:00:00");
		Duration duration = DateTimeHelper.getBusinessDuration(start, end);		
		assertEquals(4, duration.toHours());		
	}
	
	@Test
	void shouldGetDurationBetweenStartAndTheSameDay()  {
		Date start = createDate("2024-04-15 10:30:00");
		Date end = createDate("2024-04-15 15:00:00");
		Duration duration = DateTimeHelper.getBusinessDuration(start, end);		
		assertEquals(3, duration.toHours());		
	}
	
	private Date createDate(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDateTime ldt = LocalDateTime.parse(dateStr, formatter);
		
		return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
	}

	private String convertDateString(Date date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

		String formattedDate = ldt.format(formatter);
		return formattedDate;
	}
	
}

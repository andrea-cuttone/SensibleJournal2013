package dk.dtu.imm.sensible.utils;

import hirondelle.date4j.DateTime;

import java.util.Locale;
import java.util.TimeZone;

import dk.dtu.imm.sensible.stats.SpeedStats;

public class DateTimeUtils {

	public static DateTime timestampToDateTime(long timestamp) {
		return DateTime.forInstant(timestamp * 1000, TimeZone.getDefault());
	}
	
	public static DateTime makeDateWithTime(DateTime date, int hourOfTheDay, int min, int sec) {
		return new DateTime(date.getYear(), date.getMonth(), date.getDay(), hourOfTheDay, min, sec, 0);
	}
	
	public static long toTimestamp(DateTime datetime) {
		return datetime.getMilliseconds(TimeZone.getDefault()) / 1000;
	}
	
	public static String smartFormatTime(long seconds) {
		if (seconds == 0) {
			return "0m";
		}
		int h = (int)(seconds / 3600);
		int m = (int)(seconds / 60) % 60;
		if (h == 0) {
			return String.format("%dm", m);
		}
		if(m == 0) {
			return String.format("%dh", h);
		}
		return String.format("%dh %dm", h, m);
	}
	
}

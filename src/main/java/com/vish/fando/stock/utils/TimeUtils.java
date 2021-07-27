package com.vish.fando.stock.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SuppressWarnings("deprecation")
public class TimeUtils implements DesktopConstants {

	public static final ArrayList<String> timeArray = new ArrayList<>();

	static {
		int hour = 9;
		int minute = 15;
		for (int i = 0; i <= 375; i++) {
			timeArray.add(toTimeValue(hour) + ":" + toTimeValue(minute));
			minute++;
			if (minute == 60) {
				hour++;
				minute = 0;
			}
		}
	}

	public static Date getLastTradingDay() {
		Date date = getNDaysBefore(2);
		if (date.getDay() == 6) {
			date = getNDaysBefore(2);
		} else if (date.getDay() == 0) {
			date = getNDaysBefore(4);
		}
		return date;
	}

	public static Date getLastTradingDay(Date oldDate) {
		Date date = getNDaysBefore(oldDate, 1);
		if (date.getDay() == 6) {
			date = getNDaysBefore(oldDate, 2);
		} else if (date.getDay() == 0) {
			date = getNDaysBefore(oldDate, 3);
		}
		return date;
	}

	public synchronized static int getIndexOfTime(String time) {
		return timeArray.indexOf(time);
	}

	private static String toTimeValue(int timeVal) {
		if (timeVal >= 0 && timeVal <= 9)
			return "0" + timeVal;
		return String.valueOf(timeVal);
	}

	public static Date asDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date asDate(String localDateString, int minutesAdjustment) {
		try {
			LocalDateTime localDate = LocalDateTime.parse(localDateString.replaceAll("\\+\\d+$", ""));
			localDate = localDate.minusMinutes(minutesAdjustment);
			return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
		} catch (Exception e) {
			return null;
		}
	}

	public static Date asDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static LocalDate asLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static LocalDateTime asLocalDateTime(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public static Date appendOpeningTimeToDate(Date day) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return formatter.parse(dateFormatter.format(day) + " 09:15:00");
		} catch (ParseException e) {
			log.error(e);
		}
		return day;
	}

	public static Date appendClosingTimeToDate(Date day) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return formatter.parse(dateFormatter.format(day) + " 15:30:00");
		} catch (ParseException e) {
			log.error(e);
		}
		return day;
	}

	public static Date postClosingTime(Date day) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return formatter.parse(dateFormatter.format(day) + " 15:45:00");
		} catch (ParseException e) {
			log.error(e);
		}
		return day;
	}

	public static Date getNDaysBefore(long n) {
		return new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(n));
	}

	public static Date getNDaysBefore(Date date, long n) {
		return new Date(date.getTime() - TimeUnit.DAYS.toMillis(n));
	}

	public static Date getNMinsBefore(long n) {
		Date lastMinute = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(n));
		lastMinute.setSeconds(0);
		return lastMinute;
	}

	public static Date gteLastMinuteRounded() {
		Date lastMinute = new Date(System.currentTimeMillis());
		lastMinute.setSeconds(0);
		return lastMinute;
	}

	public static Date getNMinsBefore(Date date, long n) {
		return new Date(date.getTime() - TimeUnit.MINUTES.toMillis(n));
	}

	public static Date currentTime() {
		return new Date(System.currentTimeMillis());
	}

	public static String getTime() {
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
		return timeFormatter.format(currentTime());
	}

	public static String getTime(Date timeStamp) {
		if (timeStamp == null)
			return null;
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
		return timeFormatter.format(timeStamp);
	}

	public static String getTimeWithSeconds(Date timeStamp) {
		if (timeStamp == null)
			return null;
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
		return timeFormatter.format(timeStamp);
	}

	public static int getIndexOfTimeStamp(Date timeStamp) {
		if (timeStamp == null)
			return -1;
		return getIndexOfTime(getTime(timeStamp));
	}

	public static Date getDateObject(String date) {
		SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		try {
			return formatterMilliSecond.parse(date);
		} catch (ParseException e) {
			log.error(e);
		}
		return null;
	}

	public static boolean isTradingTime() {
		Date time = currentTime();
		return isBeforeTradingCloseTime(time) && isAfterTradingOpenTime(time);
	}

	public static boolean isTradingTime(Date time) {
		return isBeforeTradingCloseTime(time) && isAfterTradingOpenTime(time);
	}

	private static boolean isAfterTradingOpenTime(Date time) {
		return TimeUtils.appendOpeningTimeToDate(time).getTime() - time.getTime() < 0;
	}

	private static boolean isBeforeTradingCloseTime(Date time) {
		return time.getTime() - TimeUtils.appendClosingTimeToDate(time).getTime() < 0;
	}

	public static String getTodayDate() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormatter.format(currentTime());
	}

	public static String getStringDateNDaysBefore(int n) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormatter.format(getNDaysBefore(n));
	}

	public static String getStringDate(Date date) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormatter.format(date);
	}

	public static String getStringDay(Date date) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE");
		return dateFormatter.format(date);
	}

	public static String getCompleteDateTime() {
		SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return formatterMilliSecond.format(currentTime());
	}

	public static String getStringDateTime(Date timeStamp) {
		if (timeStamp == null)
			return null;
		SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return formatterMilliSecond.format(timeStamp);
	}

	public static String getStringDateTimeMinute(Date timeStamp) {
		if (timeStamp == null)
			return null;
		SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return formatterMilliSecond.format(timeStamp);
	}

	public static float diffInMinutesSinceSessionStart(Date date) {
		long timeDifference = date.getTime() - appendOpeningTimeToDate(date).getTime();
		return ((float) timeDifference / (1000 * 60));
	}

	public static float getDifferenceInSeconds(Date d1, Date d2) {
		long timeDifference = d2.getTime() - d1.getTime();
		return ((float) timeDifference / 1000);
	}

	public static int getDifferenceInDays(Date d1, Date d2) {
		long timeDifference = d2.getTime() - d1.getTime();
		return (int) (timeDifference / (1000 * 60 * 60 * 24));
	}

	public static int getDifferenceFromCurrentTime(Date date) {
		return (int) Math.abs(getDifferenceInSeconds(date, currentTime()));
	}

	public static String getStringDateWithDay(Date date) {
		if (date == null)
			return null;
		SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("EEEE, dd-MM-yyyy");
		return formatterMilliSecond.format(date);
	}

	public static Date maxTime(List<Date> timeStamps) {
		Date maxTime = timeStamps.get(0);
		for (Date timeStamp : timeStamps) {
			if (getDifferenceInSeconds(maxTime, timeStamp) > 0) {
				maxTime = timeStamp;
			}
		}
		return maxTime;
	}

	public static Date getDateDayObject(String date) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMMyyyy");

		Date dateObj = null;
		try {
			dateObj = dateFormatter.parse(date);
		} catch (ParseException e) {
			log.error(e);
		}
		return dateObj;
	}
}

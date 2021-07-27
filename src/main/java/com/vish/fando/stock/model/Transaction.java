package com.vish.fando.stock.model;

import java.io.Serializable;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import com.vish.fando.stock.utils.TimeUtils;
import com.zerodhatech.models.Tick;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
public class Transaction implements Serializable, Comparable<Transaction> {
	static Pattern ltpPattern = Pattern.compile("LTP=\\s+(\\d+\\.\\d+)\\,");
	static Pattern volumePattern = Pattern.compile("volume=\\s+(\\d+\\.\\d+)");

	private static final long serialVersionUID = 1L;
	private double ltp;
	private double volume;
	@EmbeddedId
	private SymbolDateModel dateTime;
	@ToString.Exclude
	private double oi;
	@ToString.Exclude
	private double buyQuantity;
	@ToString.Exclude
	private double sellQuantity;

	public String getSymbol() {
		return this.dateTime.getSymbol();
	}

	public Date getTimeStamp() {
		return TimeUtils.getDateObject(this.dateTime.getDateTime());
	}

	public Transaction(String symbol, Tick tick, String dateTime) {
		this.dateTime = new SymbolDateModel(symbol, dateTime);
		this.ltp = tick.getLastTradedPrice();
		this.volume = tick.getVolumeTradedToday();
		this.oi = tick.getOi();
		this.buyQuantity = tick.getTotalBuyQuantity();
		this.sellQuantity = tick.getTotalSellQuantity();
	}

	public Transaction(String line, String script) {

		String time = line.substring(0, 23);

		this.dateTime = new SymbolDateModel(script, time);
		Matcher m = ltpPattern.matcher(line);
		while (m.find()) {
			this.ltp = Double.parseDouble(m.group(1));
		}
		m = volumePattern.matcher(line);
		while (m.find()) {
			this.volume = Double.parseDouble(m.group(1));
		}
	}

	public boolean isInTradingHour() {
		return isBeforeTradingCloseTime() && isAfterTradingOpenTime();
	}

	private boolean isAfterTradingOpenTime() {
		return TimeUtils.appendOpeningTimeToDate(getTimeStamp()).getTime() - getTimeStamp().getTime() < 0;
	}

	private boolean isBeforeTradingCloseTime() {
		return getTimeStamp().getTime() - TimeUtils.appendClosingTimeToDate(getTimeStamp()).getTime() < 0;
	}

	public boolean isInTradingHour(int offsetMinutes) {
		return isBeforeTradingCloseTime(offsetMinutes) && isAfterTradingOpenTime(offsetMinutes);
	}

	private boolean isAfterTradingOpenTime(int offsetMinutes) {
		long openingTime = TimeUtils.appendOpeningTimeToDate(getTimeStamp()).getTime() + 1000 * 60 * offsetMinutes;
		return openingTime - getTimeStamp().getTime() < 0;
	}

	private boolean isBeforeTradingCloseTime(int offsetMinutes) {
		long closingTime = TimeUtils.appendOpeningTimeToDate(getTimeStamp()).getTime() - 1000 * 60 * offsetMinutes;
		return closingTime - TimeUtils.appendClosingTimeToDate(getTimeStamp()).getTime() < 0;
	}

	@Override
	public int compareTo(Transaction that) {
		return (int) (getTimeStamp().getTime() - that.getTimeStamp().getTime());
	}
}

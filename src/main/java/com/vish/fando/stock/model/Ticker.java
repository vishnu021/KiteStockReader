package com.vish.fando.stock.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vish.fando.stock.utils.DesktopConstants;
import com.vish.fando.stock.utils.TimeUtils;
import com.vish.fando.stock.utils.Utils;
import com.zerodhatech.models.Tick;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@Log4j2
public class Ticker implements DesktopConstants {
	private String symbol;
	@ToString.Exclude
	private long instrumentToken;
	@ToString.Exclude
	private double volumeTradedToday;
	private Date timestamp;
	private double averageVolume;
	private double ltp;
	private double oi;

	// derived variables
	private double lastMinuteLtpChanges;
	private double lastMinuteVolume;
	private double last5MinuteVolume;

	private double last1MinOiChanges;

	// history variables
	private List<Float> recentLtpChanges;
	private List<Date> tickTimeStamps;

	private List<Long> volumeChanges;
	private List<Float> ltpChanges;
	private List<Double> allOi;

	public Ticker(String symbol) {
		this.symbol = symbol;
		initialise();
	}

	public Ticker(String symbol, Tick tick, Date tickTimestamp) {
		this.symbol = symbol;
		this.instrumentToken = tick.getInstrumentToken();
		this.ltp = tick.getLastTradedPrice();
		this.volumeTradedToday = tick.getVolumeTradedToday();
		this.timestamp = tickTimestamp;
		initialise();
	}

	public Ticker(Ticker ticker) {
		this.symbol = ticker.getSymbol();
		this.ltp = ticker.getLtp();
		this.volumeTradedToday = ticker.getVolumeTradedToday();
		this.oi = ticker.getOi();
		this.timestamp = ticker.getTimestamp();
		this.lastMinuteVolume = ticker.getLastMinuteVolume();
		this.last5MinuteVolume = ticker.getLast5MinuteVolume();
		this.lastMinuteLtpChanges = ticker.getLastMinuteLtpChanges();
		this.last1MinOiChanges = ticker.getLast1MinOiChanges();
		this.averageVolume = ticker.getAverageVolume();
	}

	private void initialise() {
		this.volumeChanges = new ArrayList<>();
		this.recentLtpChanges = new ArrayList<>();
		this.tickTimeStamps = new ArrayList<>();
		this.allOi = new ArrayList<>();
		this.ltpChanges = new ArrayList<Float>();
	}

	public void setInstrumentToken(long instrumentToken) {
		log.debug("Setting instrumentToken : " + instrumentToken);
		this.instrumentToken = instrumentToken;
	}

	public void update(Tick tick, Date tickTimestamp) {
		this.timestamp = tickTimestamp;
		this.tickTimeStamps.add(tickTimestamp);

		this.recentLtpChanges.add((float) (tick.getLastTradedPrice() - this.ltp));
		this.ltpChanges.add((float) tick.getLastTradedPrice());
		this.ltp = tick.getLastTradedPrice();

		this.volumeChanges.add((long) (tick.getVolumeTradedToday() - this.volumeTradedToday));
		this.volumeTradedToday = tick.getVolumeTradedToday();

		this.allOi.add(tick.getOi());
		this.oi = tick.getOi();

		this.averageVolume = volumeTradedToday / (TimeUtils.diffInMinutesSinceSessionStart(tickTimestamp));

		this.lastMinuteVolume = calculateAverageVolume(100);
		this.last5MinuteVolume = calculateAverageVolume(500);
		this.lastMinuteLtpChanges = calculateAverageLtp(100);
		this.last1MinOiChanges = calculateOiChanges(100);
	}

	private double calculateOiChanges(int size) {
		if (allOi.size() < size + 1)
			return 0;

		double startOi = this.allOi.get(allOi.size() - size);
		double endOi = this.allOi.get(allOi.size() - 1);
		return endOi - startOi;
	}

	private double calculateAverageLtp(int size) {
		// comparing against size +1 to remove the first delta which is very large
		if (recentLtpChanges.size() < size + 1)
			return 0;
		List<Float> ltpDeltaList = this.recentLtpChanges.subList(recentLtpChanges.size() - size,
				recentLtpChanges.size());

		double averageLtp = ltpDeltaList.stream().mapToDouble(i -> i).average().getAsDouble();
		return calculateAverageVolume(size) > 0 ? averageLtp : 0;
	}

	private double calculateAverageVolume(int size) {
		// comparing against size +1 to remove the first delta which is very large
		if (volumeChanges.size() < size + 1)
			return 0;

		List<Long> recentNVolumes = volumeChanges.subList(volumeChanges.size() - size, volumeChanges.size());
		List<Date> recentNTimeStamps = tickTimeStamps.subList(tickTimeStamps.size() - size, tickTimeStamps.size());

		float differenceInSeconds = TimeUtils.getDifferenceInSeconds(recentNTimeStamps.get(0),
				recentNTimeStamps.get(recentNTimeStamps.size() - 1));

		double averageVolume = recentNVolumes.stream().mapToLong(i -> i).sum() * 60 / differenceInSeconds;
		return averageVolume > 0 ? averageVolume : 0;
	}

	public String getLatestTime() {
		return formatterMilliSecond.format(tickTimeStamps.get(tickTimeStamps.size() - 1));
	}

	public double lastMinuteLow() {
		if (ltpChanges.size() < 100)
			return ltpChanges.get(ltpChanges.size() - 1);
		List<Float> lastMinuteLTP = ltpChanges.subList(ltpChanges.size() - 100, ltpChanges.size() - 1);
		return lastMinuteLTP.stream().mapToDouble(i -> i).min().getAsDouble();
	}

	public double lastMinuteHigh() {
		if (ltpChanges.size() < 100)
			return ltpChanges.get(ltpChanges.size() - 1);
		List<Float> lastMinuteLTP = ltpChanges.subList(ltpChanges.size() - 100, ltpChanges.size() - 1);
		return lastMinuteLTP.stream().mapToDouble(i -> i).max().getAsDouble();
	}

	public double last20SecondsHigh() {
		if (ltpChanges.size() < 33)
			return ltpChanges.get(ltpChanges.size() - 1);
		List<Float> lastMinuteLTP = ltpChanges.subList(ltpChanges.size() - 33, ltpChanges.size() - 1);
		return lastMinuteLTP.stream().mapToDouble(i -> i).max().getAsDouble();
	}

	public void update(Transaction tick) {
		this.timestamp = tick.getTimeStamp();
		this.tickTimeStamps.add(tick.getTimeStamp());

		this.recentLtpChanges.add((float) (tick.getLtp() - this.ltp));
		this.ltpChanges.add((float) tick.getLtp());
		this.ltp = tick.getLtp();

		this.volumeChanges.add((long) (tick.getVolume() - this.volumeTradedToday));
		this.volumeTradedToday = tick.getVolume();

		this.allOi.add(tick.getOi());
		this.oi = tick.getOi();

		this.averageVolume = volumeTradedToday / (TimeUtils.diffInMinutesSinceSessionStart(tick.getTimeStamp()));

		this.lastMinuteVolume = calculateAverageVolume(100);
		this.last5MinuteVolume = calculateAverageVolume(500);

		this.lastMinuteLtpChanges = calculateAverageLtp(100);

		this.last1MinOiChanges = calculateOiChanges(100);
		limitListSize();
	}

	private void limitListSize() {
	}

	public int getLastOITickIndex() {
		return allOi.size() - getLastOIChangeIndex();
	}

	private int getLastOIChangeIndex() {
		int transactionIndex = 0;
		for (transactionIndex = allOi.size() - 1; transactionIndex > 0; transactionIndex--) {
			if (allOi.get(transactionIndex) != 0) {
				break;
			}
		}
		return transactionIndex;
	}

	public double getPriceChange() {
		if (ltpChanges.size() < 2)
			return 0;
		return ltpChanges.get(0) - ltpChanges.get(ltpChanges.size() - 1);
	}

	public double getAveragePriceChange() {
		if (ltpChanges.size() < 2)
			return 0;
		return (ltpChanges.get(0) - ltpChanges.get(ltpChanges.size() - 1)) / ltpChanges.size();
	}

	public float priceChangeSinceLastOiChange() {
		int transactionIndex = getLastOIChangeIndex();

		float lastPrice = ltpChanges.get(ltpChanges.size() - 1);
		float priceAtOiChange = ltpChanges.get(transactionIndex);
		return lastPrice - priceAtOiChange;
	}

	public float averagePriceChangeLTP() {
		int transactionIndex = getLastOIChangeIndex();

		Date timeStampStart = tickTimeStamps.get(ltpChanges.size() - 1);
		Date timeStampEnd = tickTimeStamps.get(transactionIndex);

		float seconds = TimeUtils.getDifferenceInSeconds(timeStampEnd, timeStampStart);
		return (priceChangeSinceLastOiChange()) / seconds;
	}

	@Override
	public String toString() {
		return String.format("[%s LTP=%7.2f,  5min=%7s, avg=%7s, oi=%7s, 1min:%7s]",
				TimeUtils.getTimeWithSeconds(timestamp), ltp, Utils.getStringRoundedPrice(last5MinuteVolume),
				Utils.getStringRoundedPrice(averageVolume), Utils.getStringRoundedPrice(oi),
				Utils.getStringRoundedPrice(last1MinOiChanges));
	}
}

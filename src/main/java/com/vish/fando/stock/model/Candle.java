package com.vish.fando.stock.model;

import java.util.Set;
import java.util.stream.Collectors;

import com.zerodhatech.models.HistoricalData;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
public class Candle implements Comparable<Candle> {
	private String timeStamp;
	private double open;
	private double high;
	private double low;
	private double close;
	@ToString.Exclude
	private long volume;
	@ToString.Exclude
	private long oi;
	@ToString.Exclude
	private Set<Candle> dataArrayList;

	public Candle(String timeStamp, double open, double high, double low, double close) {
		this.timeStamp = timeStamp;
		this.open = open;
		this.close = close;
		this.high = high;
		this.low = low;
	}

	public Candle(float startPrice, float endPrice, float maxPrice, float minPrice, long volume, long oi) {
		this.open = startPrice;
		this.close = endPrice;
		this.high = maxPrice;
		this.low = minPrice;
		this.volume = volume;
		this.oi = oi;
	}

	public Candle(HistoricalData historicalData) {
		this.timeStamp = historicalData.timeStamp;
		this.open = historicalData.open;
		this.high = historicalData.high;
		this.low = historicalData.low;
		this.close = historicalData.close;
		this.volume = historicalData.volume;
		this.oi = historicalData.oi;
		this.dataArrayList = historicalData.dataArrayList.stream().map(Candle::new).collect(Collectors.toSet());
	}

	@Override
	public int compareTo(Candle obj) {
		if (obj != null) {
			return timeStamp.compareTo(obj.timeStamp);
		}
		return 0;
	}

	@Override
	public String toString() {
		return String.format("[time=%s, o=%.1f, h=%.1f, l=%.1f, c=%.1f]",
				timeStamp.substring(0, timeStamp.length() - 8), open, high, low, close);
	}
}

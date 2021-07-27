package com.vish.fando.stock.live;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vish.fando.stock.kite.KiteService;
import com.vish.fando.stock.model.Candle;
import com.vish.fando.stock.utils.DesktopConstants;
import com.vish.fando.stock.utils.StockFiles;
import com.vish.fando.stock.utils.TimeUtils;
import com.zerodhatech.models.HistoricalData;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@Lazy
public class HistoricalDataService implements DesktopConstants {
	@Autowired
	@Lazy
	private KiteService kiteUtils;
	@Autowired
	private StockFiles fileUtils;
	@Autowired
	@Lazy
	private DataCache cache;
	private int logCounter = 0;

	public Candle currentDayCandle(String symbol, Date date) {
		HistoricalData data = kiteUtils.getDayHistoricalData(date, String.valueOf(cache.getInstrument(symbol)));
		List<Candle> candleList = data.dataArrayList.stream().map(Candle::new).collect(Collectors.toList());
		return mergeCandles(candleList);
	}

	public Candle getLastDayCandle(String symbol, Date date) {
		List<Candle> candleList = null;
		Date currentDay = date;

		while (candleList == null || candleList.size() == 0) {
			HistoricalData data = kiteUtils.getLastDayHistoricalData(currentDay,
					String.valueOf(cache.getInstrument(symbol)));
			candleList = data.dataArrayList.stream().map(Candle::new).collect(Collectors.toList());
			currentDay = TimeUtils.getLastTradingDay(currentDay);
		}
		return mergeCandles(candleList);
	}

	private Candle mergeCandles(List<Candle> candleList) {
		if (candleList == null || candleList.size() == 0)
			return null;

		Collections.sort(candleList, Comparator.naturalOrder());
		double open = candleList.get(0).getOpen();
		double high = candleList.stream().mapToDouble(c -> c.getHigh()).max().getAsDouble();
		double low = candleList.stream().mapToDouble(c -> c.getLow()).min().getAsDouble();
		double close = candleList.get(candleList.size() - 1).getClose();
		return new Candle(candleList.get(0).getTimeStamp(), open, high, low, close);
	}

	public Map<String, Set<Candle>> scanAllDayInstruments(Set<Long> instruments, Date date) {
		log.info("collecting historical data for " + instruments.size() + " instruments");
		Map<String, Set<Candle>> candleMap = new HashMap<>();
		try {
			Date fromDate = TimeUtils.appendOpeningTimeToDate(date);
			for (Long instrument : instruments) {
				try {
					candleMap.putAll(collectTillDayEndCandleData(fromDate, String.valueOf(instrument)));
				} catch (NullPointerException e) {
					log.error(e);
				}
			}
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}
		return candleMap;
	}

	public Map<String, Set<Candle>> scan5MinCandles(Set<Long> instruments, Date date) {
		log.info("collecting historical data for " + instruments.size() + " instruments");
		Map<String, Set<Candle>> candleMap = new HashMap<>();
		try {
			Date fromDate = TimeUtils.appendOpeningTimeToDate(date);
			for (Long instrument : instruments) {
				try {
					candleMap.putAll(collect5MinCandleData(fromDate, String.valueOf(instrument)));
				} catch (NullPointerException e) {
					log.error(e);
				}
			}
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}
		return candleMap;
	}

	public Map<String, Set<Candle>> scanAllDayScripts(Set<String> scripts, Date date) {
		log.info("collecting historical data for " + scripts.size() + " instruments");
		Map<String, Set<Candle>> candleMap = new HashMap<>();
		try {
			Date fromDate = TimeUtils.appendOpeningTimeToDate(date);
			for (String script : scripts) {
				try {
					long instrument = cache.getInstrument(script);
					candleMap.putAll(collectTillDayEndCandleData(fromDate, String.valueOf(instrument)));
				} catch (NullPointerException e) {
					log.error("Exception while collecting data for script : " + script + ", id : "
							+ cache.getInstrument(script), e);
				}
			}
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}
		return candleMap;
	}

	public Map<String, Set<Candle>> scanInstrumentsTillDayEnd(Set<Long> instruments, Date fromDate) {
		if (logCounter++ % 10 == 0)
			log.debug("collecting historical data for " + instruments.size() + " instruments" + " from " + fromDate);
		Map<String, Set<Candle>> candleMap = new HashMap<>();
		try {
			for (Long instrument : instruments) {
				candleMap.putAll(collectCandleData(fromDate, String.valueOf(instrument)));
			}
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}
		return candleMap;
	}

	public Map<String, Set<Candle>> scanInstrumentsTillLastMinute(Map<String, Date> scriptTimestamps) {
		Date toDate = TimeUtils.getNMinsBefore(1);
		Map<String, Set<Candle>> candleMap = new HashMap<>();
		try {
			for (String symbol : scriptTimestamps.keySet()) {
				long intrumentToken = cache.getInstrument(symbol);
				Date fromDate = scriptTimestamps.get(symbol);
				candleMap.putAll(collectCandleDataTillLastMinute(fromDate, toDate, String.valueOf(intrumentToken)));
			}
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}
		return candleMap;
	}

	public Map<String, Set<Candle>> scanInstrumentsTillLastMinute(Map<String, Date> scriptTimestamps,
			Set<String> scriptsInRange) {
		Date toDate = TimeUtils.getNMinsBefore(1);
		Map<String, Set<Candle>> candleMap = new HashMap<>();
		try {
			for (String symbol : scriptsInRange) {
				if (scriptTimestamps.containsKey(symbol)) {
					long intrumentToken = cache.getInstrument(symbol);
					Date fromDate = scriptTimestamps.get(symbol);
					candleMap.putAll(collectCandleDataTillLastMinute(fromDate, toDate, String.valueOf(intrumentToken)));
				} else {
					log.error("symbol : " + symbol + " was not initialised");
					candleMap.putAll(collectAllDayCandleData(toDate, String.valueOf(cache.getInstrument(symbol))));
				}
			}
		} catch (IOException | InterruptedException e) {
			log.error(e);
		}
		return candleMap;
	}

	public Map<String, Set<Candle>> scanInstrument(Long instrumentId, Date date) {
		return scanAllDayInstruments(Collections.singleton(instrumentId), date);
	}

	public Map<String, Set<Candle>> collectCandleDataTillLastMinute(Date fromDate, Date toDate, String instrument)
			throws JsonProcessingException, InterruptedException {
		HistoricalData historicalData = kiteUtils.getMinuteHistoricalData(fromDate, toDate, instrument);
		Set<Candle> candleList = historicalData.dataArrayList.stream().map(Candle::new).collect(Collectors.toSet());
		return Collections.singletonMap(cache.getSymbol(instrument), candleList);
	}

	private Map<String, Set<Candle>> collectCandleData(Date fromDate, String instrument)
			throws JsonProcessingException, InterruptedException {
		HistoricalData historicalData = kiteUtils.getTillDayEndMinuteHistoricalData(fromDate, instrument);
		Set<Candle> candleList = historicalData.dataArrayList.stream().map(Candle::new).collect(Collectors.toSet());
		return Collections.singletonMap(cache.getSymbol(instrument), candleList);
	}

	private Map<String, Set<Candle>> collectTillDayEndCandleData(Date fromDate, String instrument)
			throws JsonProcessingException, InterruptedException {
		String scriptName = cache.getSymbol(instrument);
		HistoricalData historicalData = kiteUtils.getTillDayEndMinuteHistoricalData(fromDate, instrument);
		fileUtils.saveHistoryData(scriptName, fromDate, historicalData);
		Set<Candle> candleList = historicalData.dataArrayList.stream().map(Candle::new).collect(Collectors.toSet());
		return Collections.singletonMap(scriptName, candleList);
	}

	private Map<String, Set<Candle>> collect5MinCandleData(Date fromDate, String instrument)
			throws JsonProcessingException, InterruptedException {
		String scriptName = cache.getSymbol(instrument);
		HistoricalData historicalData = kiteUtils.get5MinuteHistoricalData(fromDate, instrument);
//		fileUtils.saveHistoryData(scriptName, fromDate, historicalData);
		Set<Candle> candleList = historicalData.dataArrayList.stream().map(Candle::new).collect(Collectors.toSet());
		return Collections.singletonMap(scriptName, candleList);
	}

	private Map<String, Set<Candle>> collectAllDayCandleData(Date fromDate, String instrument)
			throws JsonProcessingException, InterruptedException {
		String scriptName = cache.getSymbol(instrument);
		HistoricalData historicalData = kiteUtils.getAllDayMinuteHistoricalData(fromDate, instrument);
		fileUtils.saveHistoryData(scriptName, fromDate, historicalData);
		Set<Candle> candleList = historicalData.dataArrayList.stream().map(Candle::new).collect(Collectors.toSet());
		return Collections.singletonMap(scriptName, candleList);
	}
}

package com.vish.fando.stock.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fando.stock.model.Ticker;
import com.vish.fando.stock.model.Candle;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Instrument;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class StockFiles implements DesktopConstants {
	protected ObjectMapper mapper;
	private Map<String, StringBuilder> tickerBuffer;
	int bufferLength;

	@PostConstruct
	public void initialise() {
		mapper = new ObjectMapper();
		tickerBuffer = new HashMap<>();
		bufferLength = 0;
		createDirectoryIfNotExist(filePath);
		createDirectoryIfNotExist(logPath);
	}

	public void saveInstrumentCache(List<Instrument> instruments) {
		try {
			mapper.writeValue(new File(getInstrumentFileName(0)), instruments);
		} catch (IOException e) {
			log.error(e);
		}
	}

	public List<Instrument> loadInstrumentCache(int days) {
		List<Instrument> instruments = null;
		String instrumentFileName = getInstrumentFileName(days);
		try {
			instruments = mapper.readValue(new File(getInstrumentFileName(days)),
					mapper.getTypeFactory().constructCollectionType(List.class, Instrument.class));
			log.info("Loaded instrument cache from file " + instrumentFileName);
		} catch (IOException e) {
			log.error("Instrument file not yet created");
		}
		return instruments;
	}

	public void createDirectoryIfNotExist(String path) {
		try {
			Files.createDirectories(Paths.get(path));
		} catch (IOException e) {
			log.error(e);
		}
	}

	public void saveHistoryData(String instrument, Date fromDate, HistoricalData candle) {
		if (candle == null)
			return;
		String fileName = candleFileName(instrument, fromDate);
		try {
			mapper.writeValue(new File(fileName), candle);
		} catch (IOException e) {
			log.error(e);
		}
	}

	public void saveObject(Object obj, String fileName) {
		if (obj == null)
			return;
		try {
			mapper.writeValue(new File(fileName), obj);
		} catch (IOException e) {
			log.error(e);
		}
	}

	private String candleFileName(String instrument, Date fromDate) {
		createDirectoryIfNotExist(filePath + dateFormatter.format(fromDate));
		return filePath + dateFormatter.format(fromDate) + "\\" + instrument + ".json";
	}

	private String getInstrumentFileName(int days) {
		return filePath + "instruments_" + dateFormatter.format(TimeUtils.getNDaysBefore(days)) + ".json";
	}

	public List<String> getAllFiles(String directory) {
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();
		List<String> allFiles = new ArrayList<>();

		if (listOfFiles == null || listOfFiles.length == 0) {
			throw new IllegalStateException("Offline file data not present");
		}

		for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
			if (listOfFiles[i].isFile()) {
				try {
					allFiles.add(listOfFiles[i].getCanonicalPath());
				} catch (IOException e) {
					log.error(e);
				}
			}
		}
		return allFiles;
	}

	public void publishTicks(Map<String, Ticker> tickerModelList) {
		for (String key : tickerModelList.keySet()) {
			tickerBuffer.computeIfAbsent(key, i -> new StringBuilder());
			Ticker ticker = tickerModelList.get(key);
			tickerBuffer.get(key).append(ticker.getLatestTime());
			tickerBuffer.get(key).append(TAB);
			tickerBuffer.get(key).append(ticker);
			tickerBuffer.get(key).append(NEW_LINE);
		}
		if (bufferLength++ > 25) {
			bufferLength = 0;
			writeTicksToFile();
			tickerBuffer = new HashMap<>();
		}
	}

	public void logOrders(Object... logMessage) {
		List<String> logs = Arrays.stream(logMessage).map(this::parse).collect(Collectors.toList());
		try {
			String path = filePath + "buyOrders" + dateFormatter.format(TimeUtils.currentTime()) + ".csv";
			String message = formatter.format(TimeUtils.currentTime()) + ",\t" + String.join(",\t", logs) + "\n";
			Files.write(Paths.get(path), message.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			log.error(e);
		}
	}

	private String parse(Object message) {
		if (message instanceof Double)
			return Utils.getStringRoundedPrice((double) message);
		if (message instanceof Float)
			return Utils.getStringRoundedPrice((float) message);
		return String.valueOf(message);
	}

	private String offlineCandleFolder(Date date) {
		return filePath + dateFormatter.format(date);
	}

	public Map<String, Set<Candle>> getOfflineData(Date date) {
		List<String> fileNames = getAllFiles(offlineCandleFolder(date));

		Map<String, Set<Candle>> candleMap = new HashMap<>();
		for (String fileName : fileNames) {
			Candle candle;
			try {
				candle = mapper.readValue(new File(fileName), Candle.class);
				candleMap.put(fileName.substring(fileName.lastIndexOf("\\") + 1).replace(".json", ""),
						candle.getDataArrayList());
			} catch (IOException e) {
				log.error("Failed to open/access file : " + fileName, e);
			} catch (NullPointerException e) {
				log.error("Failed to load data from file : " + fileName, e);
			}
		}
		return candleMap;
	}

	public Set<Candle> getOfflineDataForSymbol(Date date, String symbol) {
		Map<String, Set<Candle>> candleModels = getOfflineData(date);
		return candleModels.get(symbol);
	}

	private void writeTicksToFile() {
		StringBuilder message = null;
		try {
			for (String key : tickerBuffer.keySet()) {
				String path = getTickFilePath(key);
				message = tickerBuffer.get(key);
				Files.write(Paths.get(path), message.toString().getBytes(StandardCharsets.UTF_8),
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
		} catch (IOException e) {
			log.debug(message.toString());
			log.error(e);
		}
	}

	private String getTickFilePath(String key) {
		return logPath + key + ".log";
	}
}

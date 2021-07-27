package com.vish.fando.stock.utils;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.vish.fando.stock.mediator.DataCache;
import com.vish.fando.stock.mediator.TickObserver;
import com.vish.fando.stock.model.Ticker;
import com.vish.fando.stock.model.Transaction;
import com.vish.fando.stock.live.TransactionService;
import com.zerodhatech.models.Tick;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
@Lazy
@Component
public class TickerUtils implements DesktopConstants {
	@Autowired
	DataCache cache;
	@Autowired
	StockFiles fileUtils;
	@Autowired
	TransactionService transactionService;
	@Setter
	TickObserver tickObserver;

	String[] indexSymbols = {"NIFTY BANK", "NIFTY 50"};

	Map<String, Ticker> tickerModelList = new HashMap<>();

	public void updateTickData(ArrayList<Tick> ticks) {
		if (ticks.size() != 0)
			appendTickToMap(ticks);
	}

	private void appendTickToMap(ArrayList<Tick> ticks) {
		Date tickTimestamp = TimeUtils.currentTime();
		String stringTickTimestamp = TimeUtils.getStringDateTime(tickTimestamp);
		List<Transaction> transactions = new ArrayList<>();
		for (Tick tick : ticks) {
			try {
				String symbol = cache.getSymbol(tick.getInstrumentToken());
				if (!tickerModelList.containsKey(symbol))
					tickerModelList.put(symbol, new Ticker(symbol, tick, tickTimestamp));
				else
					tickerModelList.get(symbol).update(tick, tickTimestamp);

				transactions.add(new Transaction(symbol, tick, stringTickTimestamp));
			} catch (Exception e) {
				log.error("Error which adding tick " + tick + " to map", e);
			}
		}
		try {
			if (tickObserver != null)
				tickObserver.onTick(tickerModelList);
		} catch (Exception e) {
			log.error("Error on calling tick observer", e);
		}

		fileUtils.publishTicks(tickerModelList);
		transactionService.publish(transactions);
	}

	public Set<String> getScriptsInRange(double minPrice, double maxPrice) {
		Set<String> scripts = tickerModelList.entrySet().stream()
				.filter(e -> e.getValue().getLtp() > minPrice && e.getValue().getLtp() < maxPrice)
				.filter(s -> s.getKey().startsWith("BANKNIFTY")).map(e -> e.getKey()).collect(Collectors.toSet());

		scripts.addAll(Arrays.asList(indexSymbols));
		return scripts;
	}
}

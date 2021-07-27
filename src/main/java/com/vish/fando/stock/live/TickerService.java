package com.vish.fando.stock.live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.neovisionaries.ws.client.WebSocketException;
import com.vish.fando.stock.kite.KiteLogin;
import com.vish.fando.stock.mediator.Observer;
import com.vish.fando.stock.model.Candle;
import com.vish.fando.stock.model.ScriptModel;
import com.vish.fando.stock.utils.SymbolIdentifier;
import com.vish.fando.stock.utils.TickerUtils;
import com.vish.fando.stock.utils.TimeUtils;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Profile;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnError;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Lazy
public class TickerService implements Observer {
	@Autowired
	private KiteLogin kiteLogin;
	private KiteConnect kiteConnect;
	private KiteTicker tickerProvider;
	ArrayList<Long> instrumentTokens;
	@Autowired
	SymbolIdentifier symbols;
	@Autowired
	TickerUtils utils;
	@Autowired
	HistoricalDataService historyReader;

	@Value("${days_before_today}")
	private int daysBeforeToday;

	@Override
	public void initialise() {
		kiteConnect = kiteLogin.getKiteConnectSdk();
		try {
			Profile profile = kiteConnect.getProfile();
			log.info("Initialised with user name : " + profile.userName);
			addInstruments();
		} catch (IOException | JSONException | KiteException e) {
			log.error(e);
		}
	}

	public void addInstruments() {
		Map<ScriptModel, Double> symbolLtpMap = loadIntrumentsForTicker();

		instrumentTokens = symbols.getInstruments(symbolLtpMap).stream().filter(Objects::nonNull)
				.collect(Collectors.toCollection(ArrayList::new));

		log.debug("symbols : " + instrumentTokens);
		try {
			tickerUsage(instrumentTokens);
		} catch (IOException | WebSocketException | KiteException e) {
			e.printStackTrace();
		}
	}

	// Adding symbols with respect to todays opening price
	private Map<ScriptModel, Double> loadIntrumentsForTicker() {
		Map<ScriptModel, Double> symbolLtpMap = new HashMap<>();

		Candle candle1 = historyReader.currentDayCandle("NIFTY 50", TimeUtils.getNDaysBefore(daysBeforeToday));
		Candle candle2 = historyReader.currentDayCandle("NIFTY BANK", TimeUtils.getNDaysBefore(daysBeforeToday));

		symbolLtpMap.put(new ScriptModel("NIFTY 50", 50, "NIFTY"), candle1.getOpen());
		symbolLtpMap.put(new ScriptModel("NIFTY BANK", 100, "BANKNIFTY"), candle2.getOpen());
		return symbolLtpMap;
	}

	public void tickerUsage(ArrayList<Long> tokens) throws IOException, WebSocketException, KiteException {
		tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());

		tickerProvider.setOnConnectedListener(() -> {
			tickerProvider.subscribe(tokens);
			tickerProvider.setMode(tokens, KiteTicker.modeFull);
		});

		tickerProvider.setOnDisconnectedListener(() -> {
		});

		tickerProvider.setOnOrderUpdateListener(order -> log.info("ID : " + order.orderId + ", price : " + order.price
				+ ", type : " + order.orderType + ", quantity " + order.quantity + ", transaction type : "
				+ order.transactionType + " " + order.tradingSymbol + " " + order.status + "," + order.statusMessage));

		tickerProvider.setOnErrorListener(new OnError() {
			@Override
			public void onError(Exception exception) {
				log.error(exception);
			}

			@Override
			public void onError(KiteException kiteException) {
				log.error(kiteException);
			}

			@Override
			public void onError(String error) {
				log.error(error);
			}
		});

		tickerProvider.setOnTickerArrivalListener(t -> utils.updateTickData(t));
		tickerProvider.setTryReconnection(true);
		tickerProvider.setMaximumRetries(10);
		tickerProvider.setMaximumRetryInterval(30);

		tickerProvider.connect();

		boolean isConnected = tickerProvider.isConnectionOpen();
		log.info("ticker connected : " + isConnected);

		/**
		 * set mode is used to set mode in which you need tick for list of tokens.
		 * Ticker allows three modes, modeFull, modeQuote, modeLTP. For getting only
		 * last traded price, use modeLTP For getting last traded price, last traded
		 * quantity, average price, volume traded today, total sell quantity and total
		 * buy quantity, open, high, low, close, change, use modeQuote For getting all
		 * data with depth, use modeFull
		 */
		tickerProvider.setMode(tokens, KiteTicker.modeQuote);

	}

	public void subscribe(ArrayList<Long> tokens) {
		log.debug("subscribe");
		tickerProvider.subscribe(tokens);
	}

	public void usubscribe(ArrayList<Long> tokens) {
		log.debug("usubscribing");
		tickerProvider.unsubscribe(tokens);
	}

	public void unsubscribeAndDisconnect() {
		log.debug("disconnecting");
		tickerProvider.disconnect();
	}
}

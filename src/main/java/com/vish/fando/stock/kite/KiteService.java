package com.vish.fando.stock.kite;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.vish.fando.stock.live.DataCache;
import com.vish.fando.stock.live.TickerService;
import com.vish.fando.stock.mediator.Initiater;
import com.vish.fando.stock.utils.TimeUtils;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class KiteService extends Initiater {
	@Autowired
	@Lazy
	private KiteLogin kiteLogin;
	@Autowired
	@Lazy
	DataCache cache;
	@Autowired
	@Lazy
	TickerService tickerService;

	@Value("${place_orders}")
	private boolean placeOrders;

	@Value("${live_data}")
	private boolean liveData;
	private KiteConnect kiteConnect;

	@PostConstruct
	public void initialise() {
		this.attach(cache);
		if (liveData && TimeUtils.isTradingTime()) {
			log.info("Attaching ticker");
			this.attach(tickerService);
		} else {
			log.info("Not attaching ticker");
		}
	}

	public void authenticate(String requestToken) {
		this.kiteConnect = kiteLogin.authenticate(requestToken);
		notifyObservers();
	}

	public HistoricalData getLastDayHistoricalData(Date date, String token) {
		Date lastTradingDay = TimeUtils.getLastTradingDay(date);
		Date fromDate = TimeUtils.appendOpeningTimeToDate(lastTradingDay);
		Date toDate = TimeUtils.appendClosingTimeToDate(lastTradingDay);
		return getHistoricalData(fromDate, toDate, token, "60minute", false, false);
	}

	public HistoricalData getDayHistoricalData(Date date, String token) {
		Date fromDate = TimeUtils.appendOpeningTimeToDate(date);
		Date toDate = TimeUtils.appendClosingTimeToDate(date);
		return getHistoricalData(fromDate, toDate, token, "60minute", false, false);
	}

	public HistoricalData getAllDayMinuteHistoricalData(Date day, String token) {
		Date fromDate = TimeUtils.appendOpeningTimeToDate(day);
		Date toDate = TimeUtils.appendClosingTimeToDate(day);
		return getHistoricalData(fromDate, toDate, token, "minute", false, true);
	}

	public HistoricalData getTillDayEndMinuteHistoricalData(Date from, String token) {
		Date toDate = TimeUtils.appendClosingTimeToDate(from);
		return getHistoricalData(from, toDate, token, "minute", false, true);
	}

	public HistoricalData getMinuteHistoricalData(Date from, Date to, String token) {
		return getHistoricalData(from, to, token, "minute", false, true);
	}

	public HistoricalData get5MinuteHistoricalData(Date from, String token) {
		Date toDate = TimeUtils.postClosingTime(from);
		return getHistoricalData(from, toDate, token, "5minute", false, true);
	}

	public HistoricalData getHistoricalData(Date from, Date to, String token, String interval) {
		return getHistoricalData(from, to, token, interval, false, true);
	}

	public boolean buyOrder(String symbol, double price, int orderSize) {
		log.info("Order parameters -> " + new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;
			{
				put("quantity", orderSize);
				put("tradingsymbol", symbol);
				put("transactionType", "BUY");
				put("price", price);
			}
		});
		if (!placeOrders) {
			log.error("Not placing orders as it is turned off by configuration");
			return true;
		}

		OrderParams orderParams = new OrderParams();
		orderParams.quantity = orderSize;
		orderParams.orderType = Constants.ORDER_TYPE_MARKET;
		orderParams.tradingsymbol = symbol;
		orderParams.product = Constants.PRODUCT_MIS;
		orderParams.exchange = Constants.EXCHANGE_NFO;
		orderParams.transactionType = Constants.TRANSACTION_TYPE_BUY;
		orderParams.validity = Constants.VALIDITY_DAY;
		orderParams.price = price;
		orderParams.triggerPrice = 0.0;
		orderParams.tag = "v_bnf";

		Order order;
		try {
			order = kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);
			log.debug("order id : " + order.orderId);
		} catch (JSONException | IOException | KiteException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	public boolean sellOrder(String symbol, double price, int orderSize) {
		log.info("Order parameters -> " + new HashMap<String, Object>() {
			private static final long serialVersionUID = 1L;
			{
				put("quantity", orderSize);
				put("tradingsymbol", symbol);
				put("transactionType", "SELL");
				put("price", price);
			}
		});
		if (!placeOrders) {
			log.error("Not placing orders as it is turned off by configuration");
			return true;
		}
		OrderParams orderParams = new OrderParams();
		orderParams.quantity = orderSize;
		orderParams.orderType = Constants.ORDER_TYPE_MARKET;
		orderParams.tradingsymbol = symbol;
		orderParams.product = Constants.PRODUCT_MIS;
		orderParams.exchange = Constants.EXCHANGE_NFO;
		orderParams.transactionType = Constants.TRANSACTION_TYPE_SELL;
		orderParams.validity = Constants.VALIDITY_DAY;
		orderParams.price = price;
		orderParams.triggerPrice = 0.0;
		orderParams.tag = "v_bnf";

		Order order;
		try {
			order = kiteConnect.placeOrder(orderParams, Constants.VARIETY_REGULAR);
			log.debug("order id : " + order.orderId);
		} catch (JSONException | IOException | KiteException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	public HistoricalData getHistoricalData(Date from, Date to, String token, String interval, boolean continuous,
			boolean oi) {
		try {
			log.debug(
					"Collecting data for " + cache.getSymbol(Long.parseLong(token)) + " from : " + from + " to " + to);
			return kiteConnect.getHistoricalData(from, to, token, interval, continuous, oi);
		} catch (JSONException | IOException | KiteException e) {
			log.error("Error while requesting historical data (from " + from + ",to " + to + ",token " + token
					+ ", symbol : " + cache.getSymbol(token) + ", interval " + interval + ")");
			log.error(e);
		}
		return null;
	}

	public List<Instrument> getAllInstruments() {
		List<Instrument> instruments = null;
		try {
			instruments = kiteConnect.getInstruments();
			log.info("Loaded instrument cache from Kite server");
		} catch (JSONException | IOException | KiteException e) {
			log.error("Failed to load instruments from Kite server", e);
		}
		return instruments;
	}
}

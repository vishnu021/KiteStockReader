package com.vish.fando.stock.mediator;

import java.util.Map;

import com.vish.fando.stock.model.Ticker;

public interface TickObserver {
	void onTick(Map<String, Ticker> tickerModelList);
}

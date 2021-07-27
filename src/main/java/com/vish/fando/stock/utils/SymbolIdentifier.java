package com.vish.fando.stock.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.vish.fando.stock.mediator.DataCache;
import com.vish.fando.stock.model.ScriptModel;

@Component
@Lazy
public class SymbolIdentifier {
	@Autowired
	private DataCache cache;
	@Value("${symbolPrefix}")
	String nextExpiry;

	public List<String> getSymbols(Map<ScriptModel, Double> symbolLtpMap) {
		List<String> symbolList = new ArrayList<>();

		for (ScriptModel key : symbolLtpMap.keySet()) {
			symbolList.addAll(generateStrikes(key, symbolLtpMap.get(key)));
		}
		return symbolList;
	}

	public List<Long> getInstruments(Map<ScriptModel, Double> symbolLtpMap) {
		List<String> symbolList = new ArrayList<>();

		for (ScriptModel key : symbolLtpMap.keySet()) {
			symbolList.addAll(generateStrikes(key, symbolLtpMap.get(key)));
		}
		return symbolList.stream().map(s -> cache.getInstrument(s)).collect(Collectors.toList());
	}

	private List<String> generateStrikes(ScriptModel key, Double ltp) {
		List<String> symbolList = new ArrayList<>();
		symbolList.add(key.getIndexName());
		int strikePrice;
		ltp = ltp - ltp % key.getTick();

		for (int i = -7; i < 12; i++) {
			strikePrice = (int) (ltp - key.getTick() * i);
			symbolList.add(key.getPrefix() + nextExpiry + strikePrice + "PE");
		}
		for (int i = -7; i < 12; i++) {
			strikePrice = (int) (ltp + key.getTick() * i);
			symbolList.add(key.getPrefix() + nextExpiry + strikePrice + "CE");
		}
		return symbolList;
	}
}

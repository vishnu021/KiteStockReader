package com.vish.fando.stock.live;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.vish.fando.stock.kite.KiteService;
import com.vish.fando.stock.mediator.Observer;
import com.vish.fando.stock.utils.StockFiles;
import com.zerodhatech.models.Instrument;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@Lazy
public class DataCache implements Observer {
	@Autowired
	private StockFiles fileUtils;
	@Autowired
	private KiteService kiteService;

	@Value("#{new java.text.SimpleDateFormat('${dateFormat}').parse('${nextExpiryDate}')}")
	private Date expiryDate;
	@Value("${days_before_today}")
	private int daysBeforeToday;

	private List<Instrument> instruments;
	private Map<String, Long> symbolMap;
	private Map<Long, String> instrumentMap;

	@Override
	public void initialise() {

		if (instruments != null)
			return;

		List<Instrument> allInstruments = getInstruments();

		log.info("Next Expiry Date : " + expiryDate);
		List<Instrument> filteredInstruments = allInstruments.stream()
				.filter(i -> (i.getExchange().contentEquals("NSE")) || (i.getExchange().contentEquals("NFO")
						&& i.expiry != null && i.expiry.compareTo(expiryDate) == 0))
				.collect(Collectors.toList());

		log.info("Filtered instrument count : " + filteredInstruments.size());
		log.info("All dates in instruments : "
				+ filteredInstruments.stream().map(i -> i.expiry).collect(Collectors.toSet()));

		symbolMap = filteredInstruments.stream().collect(Collectors.toMap(Instrument::getTradingsymbol,
				Instrument::getInstrument_token, (token, symbol) -> token));
		instrumentMap = symbolMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	}

	public Long getInstrument(String script) {
		return this.symbolMap.get(script);
	}

	public String getSymbol(String instrument) {
		return this.instrumentMap.get(Long.parseLong(instrument));
	}

	public String getSymbol(long instrument) {
		return this.instrumentMap.get(instrument);
	}

	public List<Instrument> getInstruments() {
		if (instruments == null) {
			instruments = fileUtils.loadInstrumentCache(daysBeforeToday);
		}
		if (instruments == null) {
			instruments = kiteService.getAllInstruments();
			fileUtils.saveInstrumentCache(instruments);
		}
		return instruments;
	}
}

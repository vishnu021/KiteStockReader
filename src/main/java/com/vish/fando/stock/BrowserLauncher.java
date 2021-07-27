package com.vish.fando.stock;

import java.awt.Desktop;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class BrowserLauncher {

	@Value("${live_data}")
	private boolean liveData;

	@Value("${kite.api_key}")
	private String apiKey;

	@EventListener(ApplicationReadyEvent.class)
	public void launchBrowser() {
		if (liveData) {
			System.setProperty("java.awt.headless", "false");
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(new URI("https://kite.trade/connect/login?v=3&api_key=" + apiKey));
			} catch (Exception e) {
				log.error(e);
			}
		}
	}
}
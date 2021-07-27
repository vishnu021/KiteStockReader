package com.vish.fando.stock.kite;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Margin;
import com.zerodhatech.models.User;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@Lazy
public class KiteLogin {
	@Value("${kite.api_key}")
	private String apiKey;

	@Value("${kite.user_id}")
	private String userId;

	@Value("${kite.api_secret}")
	private String apiSecret;

	private KiteConnect kiteSdk;

	public boolean initialised = false;

	@PostConstruct
	public void initialSetup() {
		// Initialize Kiteconnect using apiKey.
		kiteSdk = new KiteConnect(apiKey, true);

		// Set userId.
		kiteSdk.setUserId(userId);
		log.info("url : " + kiteSdk.getLoginURL());
		kiteSdk.setSessionExpiryHook(() -> log.info("session expired"));
	}

	public KiteConnect getKiteConnectSdk() {
		return kiteSdk;
	}

	public KiteConnect authenticate(String requestToken) {
		try {
			initialize(requestToken);
		} catch (JSONException | IOException | KiteException e) {
			log.error(e);
		}
		return kiteSdk;
	}

	public void initialize(String requestToken) throws JSONException, IOException, KiteException {

		// Get accessToken as follows,
		User user = kiteSdk.generateSession(requestToken, apiSecret);

		// Set request token and public token which are obtained from login process.
		kiteSdk.setAccessToken(user.accessToken);
		kiteSdk.setPublicToken(user.publicToken);

		initialised = true;
		try {
			Margin margins = kiteSdk.getMargins("equity");
			log.info("Available cash : " + margins.available.cash);
			log.info("Utilised Debits : " + margins.utilised.debits);
		} catch (Exception e) {
			log.error(e);
		}
	}
}

package com.vish.fando.stock.kite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
public class KiteController {
	@Autowired
	private KiteService kiteService;
	public boolean initialised = false;

	@RequestMapping("/bankNiftyapp")
	public String authenticate(@RequestParam String request_token, @RequestParam String status,
			@RequestParam String action) {
		log.info("Got response token, status " + status + " and action " + action);
		if (initialised)
			return "Already initialised";

		initialised = true;
		kiteService.authenticate(request_token);
		return "success";
	}
}

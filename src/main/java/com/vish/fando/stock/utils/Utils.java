package com.vish.fando.stock.utils;

public class Utils {
	public static String getStringRoundedPrice(double price) {
		boolean signed = price < 0;
		price = Math.abs(price);
		String simplifiedPrice;

		if (price > (1000000))
			simplifiedPrice = getFormattedRoundPrice(price / (1000 * 1000)) + "M";
		else if (price > 1000)
			simplifiedPrice = getFormattedRoundPrice(price / 1000) + "K";
		else
			simplifiedPrice = getFormattedRoundPrice(price);

		return signed ? "-" + simplifiedPrice : simplifiedPrice;
	}

	private static String format(double price) {
		return String.format("%.2f", price);
	}

	private static String getFormattedRoundPrice(double price) {
		return format(getRoundedPrice(price));
	}

	public static float getRoundedPrice(double price) {
		return ((float) Math.round(price * 100)) / 100;
	}
}

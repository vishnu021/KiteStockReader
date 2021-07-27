package com.vish.fando.stock.utils;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public interface DesktopConstants {
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	SimpleDateFormat formatterForFile = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	SimpleDateFormat timeSecondFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
	String directory = "history_data";
	String log = "log_data";
	String filePath = Paths.get(".").normalize().toAbsolutePath() + "\\" + directory + "\\";
	String logPath = Paths.get(".").normalize().toAbsolutePath() + "\\" + log + "\\";

	String TAB = "\t";
	String NEW_LINE = "\n";
}

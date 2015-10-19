package com.evol.misc;

import java.util.Locale;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {
	public static void init() {
		init(Level.INFO);
	}

	public static void init(Level level) {
		// disable message localization
		Locale.setDefault(Locale.ENGLISH);
		// config log output format
		Properties props = System.getProperties();
		props.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tb-%1$td %1$tT [%4$s] %5$s%n");
		// set log level and format
		Logger rootLogger = Logger.getLogger("");
		rootLogger.setLevel(level);
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler h : handlers) {
			h.setLevel(level);
			h.setFormatter(new SimpleFormatter());
		}
	}

	public static void init(String level) {
		Level l = Level.parse(level);
		init(l);
	}
}
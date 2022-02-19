package com.vaguehope.morrigan.dlna.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public final class LogHelper {

	private static final Map<String, Level> LEVELS = new HashMap<String, Level>();
	static {
		//LEVELS.put("org.fourthline.cling.transport.spi", Level.TRACE); // Wire level debugging.
		LEVELS.put("org.fourthline.cling.binding.xml.ServiceDescriptorBinder", Level.ERROR);
		LEVELS.put("org.fourthline.cling.protocol.RetrieveRemoteDescriptors", Level.ERROR);
		LEVELS.put("org.fourthline.cling.transport.spi.StreamClient", Level.WARN);
		LEVELS.put("org.apache.http.impl.client.DefaultHttpClient", Level.WARN);
	}

	private LogHelper () {
		throw new AssertionError();
	}

	public static void setLoggingLevels() {
		final LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
		for (final Entry<String, Level> e : LEVELS.entrySet()) {
			loggerContext.getLogger(e.getKey()).setLevel(e.getValue());
		}
	}

}

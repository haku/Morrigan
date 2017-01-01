package com.vaguehope.morrigan.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MnLogger {

	public static MnLogger make (final Class<?> cls) {
		return new MnLogger(cls);
	}

	private final Logger logger;

	public MnLogger (final Class<?> cls) {
		this.logger = Logger.getLogger(cls.getName());
	}

	public void e (final String msg, final Object... args) {
		if (!this.logger.isLoggable(Level.SEVERE)) return;
		this.logger.log(Level.SEVERE, msg, args);
	}

	public void e (final String msg, final Throwable t) {
		if (!this.logger.isLoggable(Level.SEVERE)) return;
		this.logger.log(Level.SEVERE, msg, t);
	}

	public void w (final String msg, final Object... args) {
		if (!this.logger.isLoggable(Level.WARNING)) return;
		this.logger.log(Level.WARNING, msg, args);
	}

	public void i (final String msg, final Object... args) {
		if (!this.logger.isLoggable(Level.INFO)) return;
		this.logger.log(Level.INFO, msg, args);
	}

}

package com.vaguehope.morrigan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MnLogger {

	static {
		try {
			LogHelper.bridgeJul();
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static MnLogger make (final Class<?> cls) {
		return new MnLogger(cls);
	}

	private final Logger logger;

	public MnLogger (final Class<?> cls) {
		this.logger = LoggerFactory.getLogger(cls);
	}

	public void e (final String msg, final Object... args) {
		if (!this.logger.isErrorEnabled()) return;
		this.logger.error(msg, args);
	}

	public void e (final String msg, final Throwable t) {
		if (!this.logger.isErrorEnabled()) return;
		this.logger.error(msg, t);
	}

	public void w (final String msg, final Object... args) {
		if (!this.logger.isWarnEnabled()) return;
		this.logger.warn(msg, args);
	}

	public void i (final String msg, final Object... args) {
		if (!this.logger.isInfoEnabled()) return;
		this.logger.info(msg, args);
	}

}

package com.vaguehope.morrigan.server.util;

import java.util.logging.Level;

import org.eclipse.jetty.util.log.Logger;

import com.vaguehope.morrigan.util.ErrorHelper;

public class JettyLogger implements Logger {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(this.getClass().getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Logger getLogger (final String name) {
		return this;
	}

	@Override
	public String getName () {
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void warn (final String msg, final Object... args) {
		this.logger.log(Level.WARNING, msg, args);
	}

	@Override
	public void warn (final Throwable t) {
		this.logger.log(Level.WARNING, ErrorHelper.oneLineCauseTrace(t));
	}

	@Override
	public void warn (final String msg, final Throwable t) {
		this.logger.log(Level.WARNING, msg + " " + ErrorHelper.oneLineCauseTrace(t));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void info (final Throwable arg0) {
		// Ignore.
	}

	@Override
	public void info (final String arg0, final Object... arg1) {
		// Ignore.
	}

	@Override
	public void info (final String arg0, final Throwable arg1) {
		// Ignore.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void ignore (final Throwable t) {
		// Ignore.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean isDebugEnabled () {
		return false;
	}

	@Override
	public void setDebugEnabled (final boolean arg0) {
		// Ignore.
	}

	@Override
	public void debug (final Throwable arg0) {
		// Ignore.
	}

	@Override
	public void debug (final String arg0, final Object... arg1) {
		// Ignore.
	}

	@Override
	public void debug (final String arg0, final Throwable t) {
		// Ignore.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
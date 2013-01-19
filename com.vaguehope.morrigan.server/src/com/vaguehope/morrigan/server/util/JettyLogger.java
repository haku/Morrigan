package com.vaguehope.morrigan.server.util;

import java.util.logging.Level;

import org.eclipse.jetty.util.log.Logger;

public class JettyLogger implements Logger {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(this.getClass().getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Logger getLogger (String arg0) {
		return null;
	}
	
	@Override
	public String getName () {
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void warn (Throwable t) {
		this.logger.log(Level.WARNING, t.getMessage(), t);
	}
	
	@Override
	public void warn (String msg, Object... args) {
		this.logger.log(Level.WARNING, msg, args);
	}
	
	@Override
	public void warn (String msg, Throwable t) {
		this.logger.log(Level.WARNING, msg, t);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void info (Throwable arg0) {
		// Ignore.
	}
	
	@Override
	public void info (String arg0, Object... arg1) {
		// Ignore.
	}
	
	@Override
	public void info (String arg0, Throwable arg1) {
		// Ignore.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void ignore (Throwable t) {
		// Ignore.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean isDebugEnabled () {
		return false;
	}
	
	@Override
	public void setDebugEnabled (boolean arg0) {
		// Ignore.
	}
	
	@Override
	public void debug (Throwable arg0) {
		// Ignore.
	}
	
	@Override
	public void debug (String arg0, Object... arg1) {
		// Ignore.
	}
	
	@Override
	public void debug (String arg0, Throwable t) {
		// Ignore.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
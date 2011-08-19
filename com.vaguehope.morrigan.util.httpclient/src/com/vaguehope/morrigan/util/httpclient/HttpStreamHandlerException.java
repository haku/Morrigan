package com.vaguehope.morrigan.util.httpclient;

public class HttpStreamHandlerException extends Exception {
	
	private static final long serialVersionUID = 5212664459071370278L;
	
	public HttpStreamHandlerException (String s) {
		super(s);
	}
	
	public HttpStreamHandlerException (String s, Throwable t) {
		super(s, t);
	}
	
	public HttpStreamHandlerException(Throwable t) {
		super(t);
	}
	
}

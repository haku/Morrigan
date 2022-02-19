package com.vaguehope.morrigan.dlna;

public class DlnaException extends Exception {

	private static final long serialVersionUID = -2465051542633269977L;

	public DlnaException (final String msg, final Throwable e) {
		super(msg, e);
	}

	public DlnaException (final String msg) {
		super(msg);
	}

}

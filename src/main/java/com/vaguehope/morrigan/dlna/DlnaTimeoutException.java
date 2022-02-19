package com.vaguehope.morrigan.dlna;

import java.util.concurrent.TimeoutException;

public class DlnaTimeoutException extends DlnaException {

	private static final long serialVersionUID = -464146950980253860L;

	public DlnaTimeoutException (final String msg, final TimeoutException e) {
		super(msg, e);
	}

	public DlnaTimeoutException (final String msg) {
		super(msg);
	}

}

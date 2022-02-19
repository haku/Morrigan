package com.vaguehope.morrigan.dlna;

import org.fourthline.cling.model.message.UpnpResponse;

public class DlnaResponseException extends DlnaException {

	private static final long serialVersionUID = 2616457831774628214L;

	private final UpnpResponse upnpResponse;

	public DlnaResponseException (final String msg, final UpnpResponse upnpResponse) {
		super(msg);
		this.upnpResponse = upnpResponse;
	}

	public boolean emptyResponse() {
		return this.upnpResponse == null;
	}

	public boolean hasStatusCodeBetween(final int minInclusie, final int maxExclusive) {
		if (this.upnpResponse == null) return false;

		final int statusCode = this.upnpResponse.getStatusCode();
		return statusCode >= minInclusie && statusCode < maxExclusive;
	}

}

package com.vaguehope.morrigan.util.httpclient;

public interface Http {

	String WWW_AUTHENTICATE = "WWW-Authenticate";
	String BASIC_REALM = "Basic realm=\"Secure Area\"";

	String HEADER_AUTHORISATION = "Authorization"; // Incoming request has this.
	String HEADER_AUTHORISATION_PREFIX = "Basic "; // Incoming request starts with this.

}

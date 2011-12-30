package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.B64Code;

import com.vaguehope.morrigan.util.httpclient.Http;

public class AuthFilter implements Filter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final Logger logger = Logger.getLogger(AuthFilter.class.getName());
	
	private final AuthChecker authChecker;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public AuthFilter (AuthChecker authChecker) {
		this.authChecker = authChecker;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void init (FilterConfig arg0) throws ServletException {
		// Unused.
	}
	
	@Override
	public void destroy () {
		// Unused.
	}
	
	@Override
	public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		// Request basic auth.
		String authHeader64 = req.getHeader(Http.HEADER_AUTHORISATION);
		if (authHeader64 == null
				|| authHeader64.length() < Http.HEADER_AUTHORISATION_PREFIX.length() + 3
				|| !authHeader64.startsWith(Http.HEADER_AUTHORISATION_PREFIX)) {
			logger.fine("Auth failed: header=" + authHeader64);
			send401(resp);
			return;
		}
		
		// Verify password.
		authHeader64 = authHeader64.substring(Http.HEADER_AUTHORISATION_PREFIX.length());
		String authHeader = B64Code.decode(authHeader64, null);
		int x = authHeader.indexOf(":");
		String user = authHeader.substring(0, x);
		String pass = authHeader.substring(x + 1);
		if (user == null || pass == null || user.isEmpty() || pass.isEmpty() || !checkUser(user, pass)) {
			logger.fine("Auth failed: user=" + user + " pass=" + pass);
			send401(resp);
			return;
		}
		
		chain.doFilter(request, response);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean checkUser (@SuppressWarnings("unused") String user, String pass) {
		return this.authChecker.verifyAuth(pass);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static void send401 (HttpServletResponse resp) throws IOException {
		resp.setHeader(Http.WWW_AUTHENTICATE, Http.BASIC_REALM);
		resp.sendError(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

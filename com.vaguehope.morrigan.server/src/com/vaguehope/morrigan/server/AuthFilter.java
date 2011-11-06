package com.vaguehope.morrigan.server;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.B64Code;

public class AuthFilter implements Filter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	private static final String BASIC_REALM = "Basic realm=\"Secure Area\"";
	private static final String BASIC_HEADER_PREFIX = "Basic ";
	
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
		String authHeader64 = req.getHeader("Authorization");
		if (authHeader64 == null
				|| authHeader64.length() < BASIC_HEADER_PREFIX.length() + 3
				|| !authHeader64.startsWith(BASIC_HEADER_PREFIX)) {
			send401(resp);
			return;
		}
		
		// Verify password.
		authHeader64 = authHeader64.substring(BASIC_HEADER_PREFIX.length());
		String authHeader = B64Code.decode(authHeader64, null);
		int x = authHeader.indexOf(":");
		String user = authHeader.substring(0, x);
		String pass = authHeader.substring(x + 1);
		if (user == null || pass == null || user.isEmpty() || pass.isEmpty() || !checkUser(user, pass)) {
			send401(resp);
			return;
		}
		
		chain.doFilter(request, response);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean checkUser (String user, String pass) {
		return user.equals(pass);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static void send401 (HttpServletResponse resp) throws IOException {
		resp.setHeader(WWW_AUTHENTICATE, BASIC_REALM);
		resp.sendError(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

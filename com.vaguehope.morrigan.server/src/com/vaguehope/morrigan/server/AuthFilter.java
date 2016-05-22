package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.B64Code;

import com.vaguehope.morrigan.util.httpclient.Http;

public class AuthFilter implements Filter {

	private static final Logger logger = Logger.getLogger(AuthFilter.class.getName());

	private final AuthChecker authChecker;
	private final AuthMgr authMgr;

	public AuthFilter (final AuthChecker authChecker, final ScheduledExecutorService schEs) {
		this.authChecker = authChecker;
		this.authMgr = new AuthMgr(schEs);
	}

	@Override
	public void init (final FilterConfig arg0) throws ServletException {
		// Unused.
	}

	@Override
	public void destroy () {
		// Unused.
	}

	@Override
	public void doFilter (final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse resp = (HttpServletResponse) response;

		final Cookie tokenCookie = ServletHelper.findCookie(req, Auth.TOKEN_COOKIE_NAME);
		if (tokenCookie != null) {
			switch (this.authMgr.isValidToken(tokenCookie.getValue())) {
				case REFRESH_REQUEST:
					setTokenCookie(resp);
				case FRESH:
					chain.doFilter(request, response);
					return;
				default:
					break;
			}
		}

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
		final String authHeader = B64Code.decode(authHeader64, null);
		final int x = authHeader.indexOf(":");
		final String user = authHeader.substring(0, x);
		final String pass = authHeader.substring(x + 1);
		if (user == null || pass == null || user.isEmpty() || pass.isEmpty() || !checkUser(user, pass)) {
			logger.fine("Auth failed: user=" + user + " pass=" + pass);
			send401(resp);
			return;
		}

		setTokenCookie(resp);

		chain.doFilter(request, response);
	}

	private boolean checkUser (@SuppressWarnings("unused") final String user, final String pass) {
		return this.authChecker.verifyAuth(pass);
	}

	private static void send401 (final HttpServletResponse resp) throws IOException {
		resp.setHeader(Http.WWW_AUTHENTICATE, Http.BASIC_REALM);
		resp.sendError(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
	}

	private void setTokenCookie (final HttpServletResponse resp) throws IOException {
		final String token = this.authMgr.newToken();
		final Cookie cookie = new Cookie(Auth.TOKEN_COOKIE_NAME, token);
		cookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(Auth.MAX_TOKEN_AGE_MILLIS));
		cookie.setPath("/");
		resp.addCookie(cookie);
	}

}

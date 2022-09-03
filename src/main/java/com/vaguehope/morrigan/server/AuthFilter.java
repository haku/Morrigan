package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
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

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.NetHelper;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.httpclient.Http;

public class AuthFilter implements Filter {

	private static final Logger logger = Logger.getLogger(AuthFilter.class.getName());

	private final AuthChecker authChecker;
	private final AuthMgr authMgr;
	private final Collection<String> ipAddresses;

	public AuthFilter (final AuthChecker authChecker, final Config config, final ScheduledExecutorService schEs) throws SocketException {
		this.authChecker = authChecker;
		this.authMgr = new AuthMgr(config, schEs);

		this.ipAddresses = Collections.unmodifiableCollection(NetHelper.getIpAddressesAsStrings());
		logger.info("CORS Origins: " + this.ipAddresses);
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

		final boolean options = "OPTIONS".equals(req.getMethod());
		if (!setCorsHeaders(options, req, resp) || options) {
			return;
		}

		final Cookie tokenCookie = ServletHelper.findCookie(req, Auth.TOKEN_COOKIE_NAME);
		if (tokenCookie != null) {
			switch (this.authMgr.isValidToken(tokenCookie.getValue())) {
				case REFRESH_REQUEST:
					setTokenCookie(resp);
				//$FALL-THROUGH$
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
		final String authHeader = new String(Base64.getDecoder().decode(authHeader64), StandardCharsets.UTF_8);
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

	private boolean checkUser (final String user, final String pass) {
		return this.authChecker.verifyAuth(user, pass);
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

	/**
	 * returns false on error.
	 */
	private boolean setCorsHeaders (final boolean strict, final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final String origin = req.getHeader("Origin");
		if (StringHelper.blank(origin)) {
			if (strict) {
				ServletHelper.error(resp, 400, "Mising Origin header.");
				return false;
			}
			else {
				return true;
			}
		}

		final String host;
		try {
			host = new URI(origin).getHost();
		}
		catch (URISyntaxException e) {
			ServletHelper.error(resp, 400, "Invalid Origin header.");
			return false;
		}

		if (!this.ipAddresses.contains(host)) {
			ServletHelper.error(resp, 401, "Unauthorised CORS Origin: " + host);
			return false;
		}

		resp.setHeader("Allow", "HEAD,GET,OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", "Authorization");
		resp.setHeader("Access-Control-Allow-Origin", origin);
		resp.setHeader("Access-Control-Allow-Credentials", "true");
		resp.setHeader("Vary", "Origin");
		return true;
	}

}

package com.vaguehope.morrigan.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

public class MockHttpServletRequest implements HttpServletRequest {

	String method = "GET";
	String requestURI;
	String queryString;
	final Map<String, String> headers = new ConcurrentHashMap<>();
	final Map<String, String> params = new ConcurrentHashMap<>();

	@Override
	public String getMethod () {
		return this.method;
	}

	@Override
	public String getRequestURI () {
		return this.requestURI;
	}

	@Override
	public String getQueryString () {
		return this.queryString;
	}

	@Override
	public String getHeader (final String headerName) {
		return this.headers.get(headerName);
	}

	@Override
	public String getParameter (final String name) {
		return this.params.get(name);
	}


//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public AsyncContext getAsyncContext () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Object getAttribute (final String arg0) {
				throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Enumeration<String> getAttributeNames () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getCharacterEncoding () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getContentLength () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getContentType () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public DispatcherType getDispatcherType () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public ServletInputStream getInputStream () throws IOException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getLocalAddr () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getLocalName () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getLocalPort () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Locale getLocale () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Enumeration<Locale> getLocales () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Map<String, String[]> getParameterMap () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Enumeration<String> getParameterNames () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String[] getParameterValues (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getProtocol () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public BufferedReader getReader () throws IOException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getRealPath (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getRemoteAddr () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getRemoteHost () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getRemotePort () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public RequestDispatcher getRequestDispatcher (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getScheme () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getServerName () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getServerPort () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public ServletContext getServletContext () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isAsyncStarted () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isAsyncSupported () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isSecure () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void removeAttribute (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setAttribute (final String arg0, final Object arg1) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setCharacterEncoding (final String arg0) throws UnsupportedEncodingException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public AsyncContext startAsync () throws IllegalStateException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public AsyncContext startAsync (final ServletRequest arg0, final ServletResponse arg1) throws IllegalStateException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean authenticate (final HttpServletResponse arg0) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getAuthType () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getContextPath () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Cookie[] getCookies () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public long getDateHeader (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Enumeration<String> getHeaderNames () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Enumeration<String> getHeaders (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public int getIntHeader (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Part getPart (final String arg0) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Collection<Part> getParts () throws IOException, ServletException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getPathInfo () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getPathTranslated () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getRemoteUser () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public StringBuffer getRequestURL () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getRequestedSessionId () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getServletPath () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public HttpSession getSession () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public HttpSession getSession (final boolean arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Principal getUserPrincipal () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdFromURL () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdFromUrl () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isRequestedSessionIdValid () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isUserInRole (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void login (final String arg0, final String arg1) throws ServletException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void logout () throws ServletException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public long getContentLengthLong() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not implemented.");
	}

}

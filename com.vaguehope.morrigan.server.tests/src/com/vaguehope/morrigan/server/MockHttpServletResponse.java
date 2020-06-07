package com.vaguehope.morrigan.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.vaguehope.morrigan.util.StringHelper;

public class MockHttpServletResponse implements HttpServletResponse {

	String contentType;
	final Map<String, String> headers = new HashMap<String, String>();
	final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	final PrintWriter printWriter = new PrintWriter(this.outputStream);
	final MockServletOutputStream servletOutputStream = new MockServletOutputStream(this.outputStream);
	int status;

	public String getOutputAsString () throws IOException {
		this.printWriter.flush();
		this.servletOutputStream.flush();
		return new String(this.outputStream.toByteArray());
	}

	@Override
	public void reset () {
		this.contentType = null;
		this.headers.clear();
		this.outputStream.reset();
		this.status = 200;
	}

	@Override
	public void setContentType (final String contentType) {
		this.contentType = contentType;
	}

	@Override
	public void addHeader (final String name, final String value) {
		this.headers.put(name, value);
	}

	@Override
	public void setDateHeader (final String name, final long value) {
		this.headers.put(name, String.valueOf(value));
	}

	@Override
	public ServletOutputStream getOutputStream () throws IOException {
		return this.servletOutputStream;
	}

	@Override
	public PrintWriter getWriter () throws IOException {
		return this.printWriter;
	}

	@Override
	public void flushBuffer () throws IOException {
		this.servletOutputStream.flush();
	}

	@Override
	public int getStatus () {
		return this.status;
	}

	@Override
	public void setStatus (final int status) {
		this.status = status;
	}

	@Override
	public void setStatus (final int status, final String statusMsg) {
		this.status = status;
	}

	@Override
	public boolean isCommitted () {
		try {
			return StringHelper.notBlank(getOutputAsString());
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int getBufferSize () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getCharacterEncoding () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getContentType () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Locale getLocale () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void resetBuffer () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setBufferSize (final int arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setCharacterEncoding (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setContentLength (final int arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setLocale (final Locale arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void addCookie (final Cookie arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void addDateHeader (final String arg0, final long arg1) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void addIntHeader (final String arg0, final int arg1) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean containsHeader (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String encodeRedirectURL (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String encodeRedirectUrl (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String encodeURL (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String encodeUrl (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getHeader (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Collection<String> getHeaderNames () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public Collection<String> getHeaders (final String arg0) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void sendError (final int arg0) throws IOException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void sendError (final int arg0, final String arg1) throws IOException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void sendRedirect (final String arg0) throws IOException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setHeader (final String arg0, final String arg1) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setIntHeader (final String arg0, final int arg1) {
		throw new UnsupportedOperationException("Not implemented.");
	}

}

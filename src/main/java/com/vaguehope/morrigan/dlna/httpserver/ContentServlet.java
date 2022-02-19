package com.vaguehope.morrigan.dlna.httpserver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContentServlet extends DefaultServlet {

	private static final long serialVersionUID = -4819786280597656455L;
	private static final Logger LOG = LoggerFactory.getLogger(ContentServlet.class);

	private final FileLocator fileLocator;

	/**
	 * Fugly hack 'cos getResource()'s pathInContext has already been partly decoded.
	 */
	private final ThreadLocal<String> requestUri = new ThreadLocal<>();

	public ContentServlet (final FileLocator fileLocator) {
		this.fileLocator = fileLocator;
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final long startTime = now();
		try {
			this.requestUri.set(req.getRequestURI());
			super.doGet(req, resp);
		}
		finally {
			final long durationMillis = TimeUnit.NANOSECONDS.toMillis(now() - startTime);
			final String ranges = join(req.getHeaders(HttpHeader.RANGE.asString()), ",");
			if (ranges != null) {
				LOG.info("Request: {} {} {}ms {} ({})",
						resp.getStatus(), req.getRemoteAddr(), durationMillis, req.getRequestURI(), ranges);
			}
			else {
				LOG.info("Request: {} {} {}ms {}",
						resp.getStatus(), req.getRemoteAddr(), durationMillis, req.getRequestURI());
			}
		}
	}

	@Override
	public Resource getResource (final String pathInContext) {
		if (pathInContext.endsWith(".gz")) return null;

		final String rUri = this.requestUri.get();
		if (rUri == null) throw new IllegalStateException("No URI stored in thread-local.");
		try {
			final String uri = rUri.startsWith("/") ? rUri.substring(1) : rUri;
			final File file = this.fileLocator.idToFile(uri);
			if (file != null) {
				if (file.exists()) return Resource.newResource(file.toURI());
				LOG.info("File not found: {}", file.getAbsolutePath());
			}
			else {
				LOG.info("Resource not found: {}", rUri);
			}
		}
		catch (final MalformedURLException e) {
			LOG.info("Failed to map resource '" + rUri + "': " + e.getMessage());
		}
		catch (final IOException e) {
			LOG.info("Failed to serve resource '" + rUri + "': " + e.getMessage());
		}
		return null;
	}

	private static String join (final Enumeration<String> en, final String join) {
		if (en == null || !en.hasMoreElements()) return null;
		final StringBuilder s = new StringBuilder(en.nextElement());
		while (en.hasMoreElements()) {
			s.append(join).append(en.nextElement());
		}
		return s.toString();
	}

	private static final long NANO_ORIGIN = System.nanoTime();

	protected static long now () {
		return System.nanoTime() - NANO_ORIGIN;
	}

}

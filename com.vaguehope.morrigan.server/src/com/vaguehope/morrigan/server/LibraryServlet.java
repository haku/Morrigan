package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.config.Bundles;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.wui.MorriganWui;

/**
 * Example URLs:
 *
 * <pre>
 * GET /lib/ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js
 * </pre>
 */
public class LibraryServlet extends HttpServlet {

	public static final String CONTEXTPATH = "/lib";
	private static final long serialVersionUID = -225457065525809819L;

	private final File webLibraryDir;
	private final Set<String> libraries;

	public LibraryServlet (final BundleContext context, final Config config) throws IOException {
		this.webLibraryDir = config.getWebLibraryDir();

		final Bundle wuiBundle = Bundles.findBundle(context, MorriganWui.ID);
		final URL libraries = wuiBundle.getResource(MorriganWui.LIBRARIES);
		this.libraries = Collections.unmodifiableSet(new HashSet<String>(IoHelper.readAsList(libraries.openStream())));
		if (this.libraries.size() < 1) throw new IOException("Failed to load list of web libraries.");
		System.out.println("Libraries: " + this.libraries);
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String requestURI = req.getRequestURI();
		final String reqPath = requestURI.startsWith(CONTEXTPATH) ? requestURI.substring(CONTEXTPATH.length()) : requestURI;
		final String schemelessUri = StringHelper.removeStart(reqPath, "/");
		final String uri = "https://" + schemelessUri;

		if (!this.libraries.contains(uri)) {
			ServletHelper.error(resp, 404, "Not found: " + uri);
			return;
		}

		/*
		 * TODO
		 * calculate cache file.
		 * return it if it exists.
		 * download file.
		 * return file.
		 */

		System.out.println("req GET " + uri);
	}

}

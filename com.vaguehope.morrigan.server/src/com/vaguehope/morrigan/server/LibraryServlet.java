package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.config.Bundles;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.FileHelper;
import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.PropertiesFile;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;
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

	private static final String CONTENT_TYPE = "content-type";
	private static final long serialVersionUID = -225457065525809819L;

	private final File webLibraryDir;
	private final Set<String> libraries;

	public LibraryServlet (final BundleContext context, final Config config) throws IOException {
		this.webLibraryDir = config.getWebLibraryDir();

		final Bundle wuiBundle = Bundles.findBundle(context, MorriganWui.ID);
		final URL libraries = wuiBundle.getResource(MorriganWui.LIBRARIES);
		this.libraries = Collections.unmodifiableSet(new HashSet<String>(IoHelper.readAsList(libraries.openStream())));
		if (this.libraries.size() < 1) throw new IOException("Failed to load list of web libraries.");
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

		// TODO cache in RAM instead of always serving from disc.

		final String contentType;
		final File propFile = propFile(schemelessUri);
		final File cacheFile = cacheFile(schemelessUri);
		final PropertiesFile props = new PropertiesFile(propFile);
		if (!propFile.exists() || !cacheFile.exists()) {
			try {
				final File ftmp = File.createTempFile(cacheFile.getName(), ".tmpdl", cacheFile.getParentFile());
				try {
					final HttpResponse dlResp = HttpClient.downloadFile(new URL(uri), ftmp);
					if (dlResp.getCode() != 200) throw new IOException("HTTP " + dlResp.getCode() + " while downloading: " + uri);

					final List<String> contentTypes = dlResp.getHeader(CONTENT_TYPE);
					if (contentTypes != null && contentTypes.size() > 0) {
						contentType = contentTypes.get(0);
					}
					else {
						contentType = null;
					}

					if (contentType != null) {
						props.writeString(CONTENT_TYPE, contentType);
					}
					FileHelper.rename(ftmp, cacheFile);
				}
				finally {
					if (ftmp.exists()) ftmp.delete();
				}
			}
			catch (final HttpStreamHandlerException e) {
				throw new IOException("Failed to download: " + uri, e);
			}
		}
		else {
			contentType = props.getString(CONTENT_TYPE, null);
		}

		ServletHelper.returnFile(cacheFile, contentType, null, req.getHeader("Range"), resp);
	}

	private File cacheFile (final String name) {
		return new File(this.webLibraryDir, ChecksumHelper.md5String(name) + ".body");
	}

	private File propFile (final String name) {
		return new File(this.webLibraryDir, ChecksumHelper.md5String(name) + ".prop");
	}

}

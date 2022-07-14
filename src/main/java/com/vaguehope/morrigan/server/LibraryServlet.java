package com.vaguehope.morrigan.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.FileHelper;
import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.PropertiesFile;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

/**
 * Example URLs:
 *
 * <pre>
 * GET /lib/ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js
 * </pre>
 */
public class LibraryServlet extends HttpServlet {

	public static final String REL_CONTEXTPATH = "lib";
	public static final String CONTEXTPATH = "/" + REL_CONTEXTPATH;

	private static String LIBRARIES = "wui/libraries.txt";
	private static final String CONTENT_TYPE = "content-type";

	private static final MnLogger LOG = MnLogger.make(LibraryServlet.class);
	private static final long serialVersionUID = -225457065525809819L;

	private final File webLibraryDir;
	private final Set<String> libraries = new ConcurrentSkipListSet<>();

	/**
	 * For testing use only.
	 */
	public String upstreamScheme = "https";

	public LibraryServlet (final Config config) throws IOException {
		this(readLibraries(config.getClass()), config);
	}

	private static Set<String> readLibraries (Class<?> cls) throws IOException {
		final URL librariesUrl = cls.getClassLoader().getResource(LIBRARIES);
		if (librariesUrl == null) {
			throw new FileNotFoundException("path: " + librariesUrl);
		}
		try (final InputStream s = librariesUrl.openStream()) {
			return new HashSet<>(IoHelper.readAsList(s));
		}
	}

	public LibraryServlet (final Set<String> libraries, final Config config) throws IOException {
		this.webLibraryDir = config.getWebLibraryDir();
		this.libraries.addAll(libraries);
		if (this.libraries.size() < 1) throw new IOException("Failed to load list of web libraries.");
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String reqPath = ServletHelper.getReqPath(req, REL_CONTEXTPATH);
		final String query = req.getQueryString();

		String schemelessUri = StringHelper.removeStart(reqPath, "/");
		if (query != null) schemelessUri += "?" + query;
		final URI uri;
		try {
			uri = new URI(this.upstreamScheme + "://" + schemelessUri);
		}
		catch (final URISyntaxException e) {
			ServletHelper.error(resp, 400, "Invalid URI: " + e.toString());
			return;
		}

		// TODO cache in RAM instead of always serving from disc.

		final String contentType;
		final File propFile = propFile(schemelessUri);
		final File cacheFile = cacheFile(schemelessUri);
		final PropertiesFile props = new PropertiesFile(propFile);
		if (!propFile.exists() || !cacheFile.exists()) {
			if (!this.libraries.contains(uri.toString())) {
				ServletHelper.error(resp, 404, "Not found: " + uri);
				return;
			}

			try {
				final File ftmp = File.createTempFile(cacheFile.getName(), ".tmpdl", cacheFile.getParentFile());
				try {
					final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
					final HttpResponse dlResp = HttpClient.downloadFile(uri.toURL(), bodyBuffer);
					if (dlResp.getCode() != 200) throw new IOException("HTTP " + dlResp.getCode() + " while downloading: " + uri);

					final List<String> contentTypes = dlResp.getHeader(CONTENT_TYPE);
					if (contentTypes != null && contentTypes.size() > 0) {
						contentType = contentTypes.get(0);
					}
					else {
						contentType = null;
					}

					if (contentType != null && contentType.toLowerCase(Locale.ENGLISH).contains("css")) {
						try {
							final Map<String, String> rewrites = rewriteCss(uri, bodyBuffer);
							this.libraries.addAll(rewrites.keySet());
							LOG.i("Dynamically added: {}", rewrites);
						}
						catch (final URISyntaxException e) {
							ServletHelper.error(resp, 500, "Error rewriting URLs.");
							return;
						}
					}
					IoHelper.write(bodyBuffer, ftmp);

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

	private static final Pattern SCHEME_AND_HOST = Pattern.compile("url\\(\\s*[\"']?([^)\"'\\s]+)[\"']?\\s*\\)", Pattern.CASE_INSENSITIVE);

	/**
	 * Visible for testing.
	 */
	public static Map<String, String> rewriteCss (final URI parent, final ByteArrayOutputStream buff) throws IOException, URISyntaxException {
		final String input = buff.toString("UTF-8");
		final StringBuffer sb = new StringBuffer();
		final Map<String, String> ret = new HashMap<>();

		final Matcher m = SCHEME_AND_HOST.matcher(input);
		while (m.find()) {
			final String match = m.group(1);
			URI u = new URI(match);
			if (!u.isAbsolute()) {
				u = parent.resolve(match);
			}

			final String replacement = "/lib/" + withoutScheme(u);
			ret.put(u.toString(), replacement);

			final String replacementCss = "url(" + replacement + ")";
			m.appendReplacement(sb, replacementCss);
		}
		m.appendTail(sb);

		buff.reset();
		buff.write(sb.toString().getBytes());
		return ret;
	}

	public static String withoutScheme (final URI u) throws URISyntaxException {
		String r = new URI(null, null, u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getFragment()).toString();
		if (r.startsWith("//")) r = r.substring(2);
		return r;
	}

}

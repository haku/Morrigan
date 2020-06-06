package com.vaguehope.morrigan.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.StringHelper;

public final class ServletHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private ServletHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void error (final HttpServletResponse resp, final int status, final String msg) throws IOException {
		resp.reset();
		resp.setStatus(status);
		resp.setContentType("text/plain");
		resp.getWriter().println("HTTP Error "+status+": " + msg);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int UPLOADBUFFERSIZE = 8192;
	//private static final long DEFAULT_ASSET_EXPIRY_SECONDS = TimeUnit.HOURS.toSeconds(1);

	/**
	 * Returns true if 304 was returned and no further processing is needed.
	 */
	public static boolean checkCanReturn304 (final long lastModified, final HttpServletRequest req, final HttpServletResponse resp) {
		final long time = req.getDateHeader("If-Modified-Since");
		if (time < 0) return false;
		if (time < lastModified) return false;

		resp.reset();
		resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		return true;
	}

	/**
	 * @param name if specified then the client will be hinted that this is a download.
	 */
	public static void prepForReturnFile (final long length, final long lastModified, final String contentType, final String downloadName, final HttpServletResponse response) {
		response.reset();
		if (StringHelper.notBlank(contentType)) {
			response.setContentType(contentType);
		}
		else {
			response.setContentType("application/octet-stream");
		}
		if (StringHelper.notBlank(downloadName)) {
			response.addHeader("Content-Description", "File Transfer");
			response.addHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
		}
		response.setDateHeader("Last-Modified", lastModified);
		//response.addHeader("Cache-Control", "max-age=" + DEFAULT_ASSET_EXPIRY_SECONDS);
		response.addHeader("Pragma", "public");
		if (length > 0) response.addHeader("Content-Length", String.valueOf(length));
	}

	public static void returnFile (final File file, final String contentType, final String downloadName, final String rangeHeader, final HttpServletResponse response) throws IOException {
		final InputStream is = new BufferedInputStream(new FileInputStream(file));
		try {
			returnFile(is, file.length(), file.lastModified(), contentType, downloadName, rangeHeader, response);
		}
		finally {
			is.close();
		}
	}

	public static void returnFile (final InputStream is, final long length, final long lastModified, final String contentType, final String downloadName, final String rangeHeader, final HttpServletResponse response) throws IOException {
		long responseLength = length;

		final Long rangeOffset = parseRangeHeader(response, rangeHeader);
		if (rangeOffset != null) {
			if (rangeOffset < 0) {
				return;
			}
			if (rangeOffset > 0) {
				IoHelper.skipReliably(is, rangeOffset);
				responseLength -= rangeOffset;
			}
		}

		prepForReturnFile(responseLength, lastModified, contentType, downloadName, response);

		if (rangeOffset != null) {
			response.addHeader("Content-Range", String.format("bytes %s-%s/%s", rangeOffset, length - 1, length));
			response.setStatus(206);
		}

		OutputStream os = null;
		try {
			os = response.getOutputStream();
			final byte[] buffer = new byte[UPLOADBUFFERSIZE];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush();
			response.flushBuffer();
		}
		catch (final org.eclipse.jetty.io.EofException e) {
			// This happens when the client goes away, its not worth reporting.
		}
		finally {
			IoHelper.closeQuietly(os);
		}

	}

	private static final String RANGE_UNIT_BYTES = "bytes=";

	/**
	 * Only supports:
	 * Range: <unit>=<range-start>-
	 *
	 * Returns 0 if no header.
	 * Returns -1 if malformed.
	 */
	private static Long parseRangeHeader (final HttpServletResponse response, final String rangeHeader) throws IOException {
		if (StringHelper.blank(rangeHeader)) {
			return null;
		}

		if (!rangeHeader.startsWith(RANGE_UNIT_BYTES)) {
			error(response, 400, "Unsupported Range header, wrong units.");
			return -1L;
		}

		final String val = rangeHeader.substring(RANGE_UNIT_BYTES.length());
		final int firstDash = val.indexOf('-');
		if (firstDash < val.length() - 1) {
			error(response, 400, "Unsupported Range header, not a single range.");
			return -1L;
		}

		final String number = val.substring(0, firstDash);
		try {
			return Long.valueOf(number);
		}
		catch (final NumberFormatException e) {
			error(response, 400, "Unsupported Range header, not a number.");
			return -1L;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static boolean readParamBoolean (final HttpServletRequest req, final String name, final boolean defRet) {
		final Boolean b = readParamBoolean(req, name);
		if (b != null) return b;
		return defRet;
	}

	public static Boolean readParamBoolean (final HttpServletRequest req, final String name) {
		final String raw = StringHelper.downcase(StringHelper.trimToNull(req.getParameter(name)));
		if ("true".equals(raw)) {
			return true;
		}
		else if ("false".equals(raw)) {
			return false;
		}
		return null;
	}

	public static int readParamInteger (final HttpServletRequest req, final String name, final int defVal) {
		final Integer ret = readParamInteger(req, name);
		if (ret != null) return ret.intValue();
		return defVal;
	}

	public static Integer readParamInteger (final HttpServletRequest req, final String name) {
		final String raw = StringHelper.trimToNull(req.getParameter(name));
		if (raw == null) return null;
		try {
			return Integer.parseInt(raw);
		}
		catch (final NumberFormatException e) {
			return null;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static Cookie findCookie (final HttpServletRequest req, final String name) {
		final Cookie[] cookies = req.getCookies();
		if (cookies == null) return null;

		for (final Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) return cookie;
		}

		return null;
	}

}

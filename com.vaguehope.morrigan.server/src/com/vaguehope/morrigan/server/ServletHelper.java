package com.vaguehope.morrigan.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		long time = req.getDateHeader("If-Modified-Since");
		if (time < 0) return false;
		if (time < lastModified) return false;

		resp.reset();
		resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		return true;
	}

	public static void prepForReturnFile (final String name, final long length, final HttpServletResponse response) {
		prepForReturnFile(name, length, System.currentTimeMillis(), response);
	}

	/**
	 * @param name if specified then the client will be hinted that this is a download.
	 */
	public static void prepForReturnFile (final String name, final long length, final long lastModified, final HttpServletResponse response) {
		response.reset();
		if (name != null) {
			response.setContentType("application/octet-stream");
			response.addHeader("Content-Description", "File Transfer");
			response.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
		}
		response.addHeader("Content-Transfer-Encoding", "binary");
		response.setDateHeader("Last-Modified", lastModified);
		//response.addHeader("Cache-Control", "max-age=" + DEFAULT_ASSET_EXPIRY_SECONDS);
		response.addHeader("Pragma", "public");
		if (length > 0) response.addHeader("Content-Length", String.valueOf(length));
	}

	public static void returnFile (final File file, final HttpServletResponse response) throws IOException {
		returnFile(file, response, true);
	}

	public static void returnFile (final File file, final HttpServletResponse response, final boolean asDownload) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		try {
			String name = asDownload ? file.getName() : null;
			returnFile(is, name, file.length(), file.lastModified(), response);
		}
		finally {
			is.close();
		}
	}

	public static void returnFile (final InputStream is, final String name, final long length, final long lastModified, final HttpServletResponse response) throws IOException {
		prepForReturnFile(name, length, lastModified, response);
		OutputStream os = null;
		try {
			os = response.getOutputStream();
			byte[] buffer = new byte[UPLOADBUFFERSIZE];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.flush();
			response.flushBuffer();
		}
		finally {
			if (os != null) os.close();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static boolean readParamBoolean (final HttpServletRequest req, final String name, final boolean defRet) {
		Boolean b = readParamBoolean(req, name);
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
}

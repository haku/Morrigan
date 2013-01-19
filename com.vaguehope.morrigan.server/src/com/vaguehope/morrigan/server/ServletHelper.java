package com.vaguehope.morrigan.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public final class ServletHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private ServletHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void error (HttpServletResponse resp, int status, String msg) throws IOException {
		resp.reset();
		resp.setStatus(status);
		resp.setContentType("text/plain");
		resp.getWriter().println("HTTP Error "+status+": " + msg);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int UPLOADBUFFERSIZE = 8192;

	/**
	 * @param name if specified then the client will be hinted that this is a download.
	 */
	public static void prepForReturnFile (String name, long length, HttpServletResponse response) {
		response.reset();
		if (name != null) {
			response.setContentType("application/octet-stream");
			response.addHeader("Content-Description", "File Transfer");
			response.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
		}
		response.addHeader("Content-Transfer-Encoding", "binary");
		response.addHeader("Expires", "0");
		response.addHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		response.addHeader("Pragma", "public");
		if (length > 0) response.addHeader("Content-Length", String.valueOf(length));
	}

	public static void returnFile (File file, HttpServletResponse response) throws IOException {
		returnFile(file, response, true);
	}

	public static void returnFile (File file, HttpServletResponse response, boolean asDownload) throws IOException {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			String name = asDownload ? file.getName() : null;
			returnFile(is, name, file.length(), response);
		}
		finally {
			if (is != null) is.close();
		}
	}

	public static void returnFile (InputStream is, String name, long length, HttpServletResponse response) throws IOException {
		prepForReturnFile(name, length, response);
		OutputStream os = null;
		try { // FIXME this could be done better?
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
}

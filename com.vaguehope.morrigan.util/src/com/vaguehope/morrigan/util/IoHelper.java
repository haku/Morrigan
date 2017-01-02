package com.vaguehope.morrigan.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoHelper {

	private static final int BUFFER_SIZE = 1024 * 4;

	public static long copy (final InputStream is, final OutputStream os) throws IOException {
		final byte[] buffer = new byte[BUFFER_SIZE];
		long total = 0;
		int read = 0;
		while ((read = is.read(buffer)) != -1) {
			os.write(buffer, 0, read);
			total += read;
		}
		return total;
	}

	public static long drainStream (final InputStream is) throws IOException {
		final byte[] buffer = new byte[BUFFER_SIZE];
		long total = 0;
		int read = 0;
		while ((read = is.read(buffer)) != -1) {
			total += read;
		}
		return total;
	}

}

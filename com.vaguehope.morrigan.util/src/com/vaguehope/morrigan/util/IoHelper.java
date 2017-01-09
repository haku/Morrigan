package com.vaguehope.morrigan.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoHelper {

	private static final int BUFFER_SIZE = 1024 * 4;

	public static void closeQuietly (final Closeable c) {
		if (c == null) return;
		try {
			c.close();
		}
		catch (final IOException e) {/**/} // NOSONAR this is intentional, is in the name of the method.
	}

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

package com.vaguehope.morrigan.util;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class IoHelper {

	private static final int BUFFER_SIZE = 1024 * 4;
	private static byte[] DRAIN_BUFFER;

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

	public static void write (final InputStream is, final File file) throws IOException {
		final OutputStream os = new FileOutputStream(file);
		try {
			copy(is, os);
		}
		finally {
			closeQuietly(is);
			closeQuietly(os);
		}
	}

	public static void write (final String data, final File file) throws IOException {
		write(new ByteArrayInputStream(data.getBytes("UTF-8")), file);
	}

	/**
	 * Returns null if file does not exist.
	 */
	public static String readAsString (final File file) throws IOException {
		try {
			final FileInputStream stream = new FileInputStream(file);
			try {
				final FileChannel fc = stream.getChannel();
				final MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
				/* Instead of using default, pass in a decoder. */
				return Charset.defaultCharset().decode(bb).toString();
			}
			finally {
				stream.close();
			}
		}
		catch (final FileNotFoundException e) {
			return null;
		}
	}

	public static long drainStream (final InputStream is) throws IOException {
		if (DRAIN_BUFFER == null) {
			DRAIN_BUFFER = new byte[BUFFER_SIZE];
		}

		long total = 0;
		int read = 0;
		while ((read = is.read(DRAIN_BUFFER)) != -1) {
			total += read;
		}
		return total;
	}

	public static void skipReliably (final InputStream is, final long count) throws IOException {
		if (count < 1) return;

		if (DRAIN_BUFFER == null) {
			DRAIN_BUFFER = new byte[BUFFER_SIZE];
		}

		long unskipped = count;
		int read = 0;
		while (unskipped > 0) {
			read = is.read(DRAIN_BUFFER, 0, (int) Math.min(unskipped, BUFFER_SIZE));
			if (read < 0) break;
			unskipped -= read;
		}

		if (unskipped > 0) throw new EOFException("No more bytes to skip.");
	}

}

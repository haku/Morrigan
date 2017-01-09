package com.vaguehope.morrigan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessHelper {

	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final long EXIT_POLL_INTERVAL_MILLIS = 200L;

	public static List<String> runAndWait (final String... cmd) throws IOException {
		final List<String> ret = new ArrayList<String>();
		runAndWait(cmd, new Listener<String>() {
			@Override
			public void onAnswer (final String line) {
				ret.add(line);
			}
		});
		return ret;
	}

	public static void runAndWait (final String[] cmd, final Listener<String> onLine) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		final Process p = pb.start();
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					onLine.onAnswer(line);
				}
			}
			finally {
				IoHelper.closeQuietly(reader);
			}
		}
		finally {
			p.destroy();
			try {
				final int result = waitFor(p, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				if (result != 0) {
					throw new IOException("Process failed: cmd " + Arrays.toString(cmd) + " result=" + result);
				}
			}
			catch (final IllegalThreadStateException e) {
				throw new IOException("Process did not stop when requested: " + Arrays.toString(cmd));
			}
		}
	}

	public static int waitFor (final Process p, final int timeout, final TimeUnit unit) {
		final long startNanos = System.nanoTime();
		while (true) {
			try {
				return p.exitValue();
			}
			catch (final IllegalThreadStateException e) {
				if (TimeUnit.NANOSECONDS.convert(timeout, unit) < System.nanoTime() - startNanos) {
					throw e; // Timed out.
				}
				try {
					Thread.sleep(EXIT_POLL_INTERVAL_MILLIS);
				}
				catch (final InterruptedException e1) {/* Ignore. */}
			}
		}
	}

}

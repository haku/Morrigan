package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import com.vaguehope.morrigan.util.ProcessHelper;

public class Ffprobe {

	private static Boolean isAvailable = null;

	public static boolean isAvailable () {
		if (isAvailable == null) {
			try {
				isAvailable = ProcessHelper.runAndWait("ffprobe", "-version").size() > 0;
			}
			catch (final IOException e) {
				isAvailable = false;
			}
		}
		return isAvailable.booleanValue();
	}

	private static void checkAvailable() throws IOException {
		if (!isAvailable()) throw new IOException("ffprobe not avilable.");
	}

	/**
	 * Will not return null.
	 */
	public static FfprobeInfo inspect (final File inFile) throws IOException {
		checkAvailable();
		final FfprobeParser parser = new FfprobeParser();
		ProcessHelper.runAndWait(new String[] {
				"ffprobe",
				"-hide_banner",
				"-show_streams",
				"-print_format", "flat",
				inFile.getAbsolutePath()
		}, parser);
		return parser.build();
	}

}

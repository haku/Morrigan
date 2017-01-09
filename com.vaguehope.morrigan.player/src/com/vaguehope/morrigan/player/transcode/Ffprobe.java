package com.vaguehope.morrigan.player.transcode;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.vaguehope.morrigan.util.Listener;
import com.vaguehope.morrigan.util.ProcessHelper;
import com.vaguehope.morrigan.util.StringHelper;

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

	public static Set<String> streamCodecs(final File inFile) throws IOException {
		checkAvailable();
		final Set<String> codecs = runFfprobeStreams(inFile, ".codec_name");
		if (codecs.size() < 1) throw new IOException("ffprobe found no codecs in file: " + inFile.getAbsolutePath());
		return codecs;
	}

	public static boolean has10BitColour(final File inFile) throws IOException {
		checkAvailable();
		for (final String profile : streamProfiles(inFile)) {
			if (profile.endsWith(" 10")) return true;
		}
		return false;
	}

	public static Set<String> streamProfiles(final File inFile) throws IOException {
		checkAvailable();
		final Set<String> profiles = runFfprobeStreams(inFile, ".profile");
		if (profiles.size() < 1) throw new IOException("ffprobe found no profiles in file: " + inFile.getAbsolutePath());
		return profiles;
	}

	private static Set<String> runFfprobeStreams (final File inFile, final String keySuffex) throws IOException {
		final Set<String> values = new LinkedHashSet<String>();
		ProcessHelper.runAndWait(new String[] {
				"ffprobe",
				"-hide_banner",
				"-show_streams",
				"-print_format", "flat",
				inFile.getAbsolutePath()
		}, new Listener<String>() {
			@Override
			public void onAnswer (final String line) {
				final String[] parts = StringHelper.splitOnce(line, '=');
				if (parts != null && parts[0].endsWith(keySuffex)) {
					values.add(StringHelper.removeEndQuotes(parts[1]).toLowerCase(Locale.ENGLISH));
				}
			}
		});
		return values;
	}

}

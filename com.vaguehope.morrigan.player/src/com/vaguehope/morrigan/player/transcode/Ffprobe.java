package com.vaguehope.morrigan.player.transcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.vaguehope.morrigan.util.StringHelper;

public class Ffprobe {

	public static Set<String> streamCodecs(final File inFile) throws IOException {
		final Set<String> codecs = runFfprobeStreams(inFile, ".codec_name");
		if (codecs.size() < 1) throw new IOException("ffprobe found no codecs in file: " + inFile.getAbsolutePath());
		return codecs;
	}

	public static boolean has10BitColour(final File inFile) throws IOException {
		for (final String profile : streamProfiles(inFile)) {
			if (profile.endsWith(" 10")) return true;
		}
		return false;
	}

	public static Set<String> streamProfiles(final File inFile) throws IOException {
		final Set<String> profiles = runFfprobeStreams(inFile, ".profile");
		if (profiles.size() < 1) throw new IOException("ffprobe found no profiles in file: " + inFile.getAbsolutePath());
		return profiles;
	}

	private static Set<String> runFfprobeStreams (final File inFile, final String keySuffex) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(new String[] {
				"ffprobe",
				"-hide_banner",
				"-show_streams",
				"-print_format", "flat",
				inFile.getAbsolutePath()
		});
		pb.redirectErrorStream(true);
		final Process p = pb.start();
		try {
			final Set<String> values = new LinkedHashSet<String>();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] parts = StringHelper.splitOnce(line, '=');
				if (parts != null && parts[0].endsWith(keySuffex)) {
					values.add(StringHelper.removeEndQuotes(parts[1]).toLowerCase(Locale.ENGLISH));
				}
			}
			return values;
		}
		finally {
			p.destroy();
		}
	}

}

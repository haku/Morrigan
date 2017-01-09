package com.vaguehope.morrigan.player.transcode;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.vaguehope.morrigan.util.Listener;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;

public class FfprobeParser implements Listener<String> {

	private static final MnLogger LOG = MnLogger.make(FfprobeParser.class);

	private final Set<String> codecs = new LinkedHashSet<String>();
	private final Set<String> profiles = new LinkedHashSet<String>();
	private Long durationMillis = null;

	@Override
	public void onAnswer (final String line) {
		final String[] parts = StringHelper.splitOnce(line, '=');
		if (parts == null) return;

		if (parts[0].endsWith(".codec_name")) {
			this.codecs.add(StringHelper.removeEndQuotes(parts[1]).toLowerCase(Locale.ENGLISH));
		}
		else if (parts[0].endsWith(".profile")) {
			this.profiles.add(StringHelper.removeEndQuotes(parts[1]).toLowerCase(Locale.ENGLISH));
		}
		else if (parts[0].endsWith(".duration")) {
			try {
				final double seconds = Double.parseDouble(StringHelper.removeEndQuotes(parts[1]));
				final long millis = (long) (seconds * 1000d);
				if (this.durationMillis == null || millis > this.durationMillis) this.durationMillis = millis;
			}
			catch (final NumberFormatException e) {
				LOG.w("Failed to parse {0}: {1}", line, e.toString());
			}
		}
	}

	public FfprobeInfo build () {
		return new FfprobeInfo(this.codecs, this.profiles, this.durationMillis);
	}

}

package com.vaguehope.morrigan.transcode;

import java.util.Collections;
import java.util.Set;

public class FfprobeInfo {

	private final long fileLastModified;
	private final Set<String> codecs;
	private final Set<String> profiles;
	private final Long durationMillis;

	FfprobeInfo (final long fileLastModified, final Set<String> codecs, final Set<String> profiles, final Long durationMillis) {
		this.fileLastModified = fileLastModified;
		this.codecs = Collections.unmodifiableSet(codecs);
		this.profiles = Collections.unmodifiableSet(profiles);
		this.durationMillis = durationMillis;
	}

	public long getFileLastModified () {
		return this.fileLastModified;
	}

	public Set<String> getCodecs () {
		return this.codecs;
	}

	public Set<String> getProfiles () {
		return this.profiles;
	}

	public boolean has10BitColour () {
		for (final String profile : this.profiles) {
			if (profile.endsWith(" 10")) return true;
		}
		return false;
	}

	public Long getDurationMillis () {
		return this.durationMillis;
	}

}

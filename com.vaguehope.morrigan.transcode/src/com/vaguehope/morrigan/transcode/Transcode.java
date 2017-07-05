package com.vaguehope.morrigan.transcode;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public enum Transcode {
	NONE("", "No Transcode") {
		@Override
		public TranscodeProfile profileForItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws IOException {
			return null;
		}
	},
	AUDIO_ONLY("audio_only", "Audio Only") {
		@Override
		public TranscodeProfile profileForItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws IOException {
			if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "video")) {
				return new AudioStreamExtractOrTranscode(list, item, this, MimeType.MP3, COMMON_AUDIO_TYPES);
			}
			return null;
		}
	},
	COMMON_AUDIO_ONLY("common_audio_only", "Common Audio Only") {
		@Override
		public TranscodeProfile profileForItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws IOException {
			final String itemMimeType = item.getMimeType();
			final String itemMimeTypeLower = itemMimeType != null ? itemMimeType.toLowerCase(Locale.ENGLISH) : null;

			if (COMMON_AUDIO_TYPES_STRINGS.contains(itemMimeTypeLower)) return null;
			return new AudioStreamExtractOrTranscode(list, item, this, MimeType.MP3, COMMON_AUDIO_TYPES);
		}
	},
	MP3_ONLY("mp3_only", "MP3 Only") {
		@Override
		public TranscodeProfile profileForItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws IOException {
			if (MimeType.MP3.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				return null;
			}
			return new AudioStreamExtractOrTranscode(list, item, this, MimeType.MP3);
		}
	},
	MP4_COMPATIBLE("mp4_compatible", "MP4 Compatible") {
		@Override
		public TranscodeProfile profileForItem (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws IOException {
			if (MimeType.MP4.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				// Use existence of cache file to reduce calls to ffprobe.
				if (Mp4CompatibleTranscode.cacheFileMp4(item, this).exists()
						|| Ffprobe.inspect(item.getFile()).has10BitColour()) {
					return new Mp4CompatibleTranscode(list, item, this);
				}
				return null;
			}
			else if (MimeType.M4A.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				return null;
			}
			else if (MimeType.MP3.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				// Assume anything that can play MP4 can also play MP3.
				return null;
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "video")) {
				return new Mp4CompatibleTranscode(list, item, this);
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "audio")) {
				return new AudioStreamExtractOrTranscode(list, item, this, MimeType.M4A);
			}
			return null;
		}
	};

	protected static final Set<MimeType> COMMON_AUDIO_TYPES = Collections.unmodifiableSet(EnumSet.of(
			MimeType.MP3, MimeType.M4A, MimeType.OGG, MimeType.OGA, MimeType.FLAC, MimeType.WAV));

	protected static final Set<String> COMMON_AUDIO_TYPES_STRINGS;
	static {
		final Set<String> s = new HashSet<String>();
		for (final MimeType mt : COMMON_AUDIO_TYPES) {
			s.add(mt.getMimeType().toLowerCase(Locale.ENGLISH));
		}
		COMMON_AUDIO_TYPES_STRINGS = Collections.unmodifiableSet(s);
	}

	private final String symbolicName;
	private final String uiName;

	private Transcode (final String symbolicName, final String uiName) {
		this.symbolicName = symbolicName;
		this.uiName = uiName;
	}

	public String getSymbolicName () {
		return this.symbolicName;
	}

	@Override
	public String toString () {
		return this.uiName;
	}

	/**
	 * Returns null if no transcode is required.
	 */
	public abstract TranscodeProfile profileForItem (IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) throws IOException;

	/**
	 * Never returns null.
	 */
	public static Transcode parse (final String str) {
		if (str == null) return NONE;
		for (final Transcode t : values()) {
			if (t.symbolicName.equalsIgnoreCase(str)) return t;
		}
		throw new IllegalArgumentException("Unsupported transcode: " + str);
	}

}

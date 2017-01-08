package com.vaguehope.morrigan.player.transcode;

import java.io.IOException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public enum Transcode {
	NONE("", "No Transcode") {
		@Override
		public TranscodeProfile profileForItem (final IMediaItem item) throws IOException {
			return null;
		}
	},
	AUDIO_ONLY("audio_only", "Audio Only") {
		@Override
		public TranscodeProfile profileForItem (final IMediaItem item) throws IOException {
			if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "video")) {
				return new AudioStreamExtractOrTranscode(item, this);
			}
			return null;
		}
	},
	MP4_COMPATIBLE("mp4_compatible", "MP4 Compatible") {
		@Override
		public TranscodeProfile profileForItem (final IMediaItem item) throws IOException {
			if (MimeType.MP4.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				// Use existence of cache file to reduce calls to ffprobe.
				if (Mp4CompatibleTranscode.cacheFileMp4(item, this).exists()
						|| Ffprobe.has10BitColour(item.getFile())) {
					return new Mp4CompatibleTranscode(item, this);
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
				return new Mp4CompatibleTranscode(item, this);
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "audio")) {
				return new AudioOnlyM4aTranscode(item, this);
			}
			return null;
		}
	};

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
	public abstract TranscodeProfile profileForItem (final IMediaItem item) throws IOException;

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

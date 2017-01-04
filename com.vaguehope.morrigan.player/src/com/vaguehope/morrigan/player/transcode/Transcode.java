package com.vaguehope.morrigan.player.transcode;

import java.io.IOException;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public enum Transcode {
	AUDIO_ONLY("audio_only", "Audio Only") {
		@Override
		public TranscodeProfile profileForItem (final IMediaItem item) throws IOException {
			if (MimeType.MP4.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				return new Mp4StreamExtract(item, this);
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "video")) {
				return new GenericMp3Transcode(item, this);
			}
			return null;
		}
	},
	NONE("", "None") {
		@Override
		public TranscodeProfile profileForItem (final IMediaItem item) throws IOException {
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

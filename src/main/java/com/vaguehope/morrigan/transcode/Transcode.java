package com.vaguehope.morrigan.transcode;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;

public enum Transcode {
	NONE("", "No Transcode") {
		@Override
		public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItemList list, final IMediaItem item) throws IOException {
			return null;
		}

		@Override
		public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItem item, final ItemTags tags) throws IOException {
			return null;
		}
	},
	COMMON_AUDIO_ONLY("common_audio_only", "Common Audio Only") {
		@Override
		public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItem item, final ItemTags tags) throws IOException {
			final String itemMimeType = item.getMimeType();
			final String itemMimeTypeLower = itemMimeType != null ? itemMimeType.toLowerCase(Locale.ENGLISH) : null;

			if (COMMON_AUDIO_TYPES_STRINGS.contains(itemMimeTypeLower) && !ConfigTag.isAnyPresent(tags)) {
				return null;
			}
			return new AudioStreamExtractOrTranscode(context, item, tags, this, MimeType.M4A, COMMON_AUDIO_TYPES);
		}
	},
	MOBILE_AUDIO("mobile_audio", "Mobile Audio") {
		@Override
		public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItem item, final ItemTags tags) throws IOException {
			final String itemMimeType = item.getMimeType();
			final String itemMimeTypeLower = itemMimeType != null ? itemMimeType.toLowerCase(Locale.ENGLISH) : null;

			if (MOBILE_AUDIO_TYPES_STRINGS.contains(itemMimeTypeLower) && !ConfigTag.isAnyPresent(tags)) {
				return null;
			}
			return new AudioStreamExtractOrTranscode(context, item, tags, this, MimeType.M4A, MOBILE_AUDIO_TYPES);
		}
	},
	MP3_ONLY("mp3_only", "MP3 Only") {
		@Override
		public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItem item, final ItemTags tags) throws IOException {
			if (MimeType.MP3.getMimeType().equalsIgnoreCase(item.getMimeType()) && !ConfigTag.isAnyPresent(tags)) {
				return null;
			}
			return new AudioStreamExtractOrTranscode(context, item, tags, this, MimeType.MP3);
		}
	},
	MP4_COMPATIBLE("mp4_compatible", "MP4 Compatible") {
		@Override
		public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItem item, final ItemTags tags) throws IOException {
			final boolean hasConfigTags = ConfigTag.isAnyPresent(tags);

			if (MimeType.MP4.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				// Use existence of cache file to reduce calls to ffprobe.
				if (hasConfigTags
						|| Mp4CompatibleTranscode.cacheFileMp4(context, item, this).exists()
						|| context.ffprobeCache.inspect(item.getFile()).has10BitColour()) {
					return new Mp4CompatibleTranscode(context, item, tags, this);
				}
				return null;
			}
			else if (!hasConfigTags && MimeType.M4A.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				return null;
			}
			else if (!hasConfigTags && MimeType.MP3.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				// Assume anything that can play MP4 can also play MP3.
				return null;
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "video")) {
				return new Mp4CompatibleTranscode(context, item, tags, this);
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "audio")) {
				return new AudioStreamExtractOrTranscode(context, item, tags, this, MimeType.M4A);
			}

			if (hasConfigTags) {
				LOG.w("Track has config tags but can't figure out the right profile: {}", item);
			}
			return null;
		}
	};

	protected static final Set<MimeType> COMMON_AUDIO_TYPES = Collections.unmodifiableSet(EnumSet.of(
			MimeType.MP3, MimeType.M4A, MimeType.OGG, MimeType.OGA, MimeType.OPUS, MimeType.FLAC, MimeType.WAV));
	protected static final Set<String> COMMON_AUDIO_TYPES_STRINGS = COMMON_AUDIO_TYPES.stream()
			.map(m -> m.getMimeType().toLowerCase(Locale.ENGLISH))
			.collect(Collectors.toSet());

	protected static final Set<MimeType> MOBILE_AUDIO_TYPES = Collections.unmodifiableSet(EnumSet.of(
			MimeType.MP3, MimeType.M4A, MimeType.OGG, MimeType.OGA, MimeType.OPUS));
	protected static final Set<String> MOBILE_AUDIO_TYPES_STRINGS = MOBILE_AUDIO_TYPES.stream()
			.map(m -> m.getMimeType().toLowerCase(Locale.ENGLISH))
			.collect(Collectors.toSet());

	private static final MnLogger LOG = MnLogger.make(Transcode.class);

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

	public TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItemList list, final IMediaItem item) throws IOException, MorriganException {
		return profileForItem(context, item, list != null ? list.readTags(item) : ItemTags.EMPTY);
	}

	/**
	 * Returns null if no transcode is required.
	 * Ideally should be quite quick as its used to determine if a transcode is needed when building API list responses.
	 */
	public abstract TranscodeProfile profileForItem (final TranscodeContext context, final IMediaItem item, final ItemTags tags) throws IOException;

	/**
	 * Case-insensitive.
	 * Never returns null.
	 */
	public static Transcode parse (final String str) {
		final Transcode t = parseOrNull(str);
		if (t != null) return t;
		throw new IllegalArgumentException("Unknown transcode: " + str);
	}

	/**
	 * Case-insensitive.
	 */
	public static Transcode parseOrNull (final String str) {
		if (StringHelper.blank(str)) return NONE;
		for (final Transcode t : values()) {
			if (t.symbolicName.equalsIgnoreCase(str)) return t;
		}
		for (final Transcode t : values()) {
			if (t.uiName.equalsIgnoreCase(str)) return t;
		}
		return null;
	}

	/**
	 * Does not include first entry.
	 */
	public static String[] symbolicNames() {
		final String[] ret = new String[values().length - 1];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = values()[i + 1].getSymbolicName();
		}
		return ret;
	}

}

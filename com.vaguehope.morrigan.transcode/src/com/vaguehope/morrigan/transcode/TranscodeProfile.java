package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.TimeHelper;

public abstract class TranscodeProfile {

	private static final MnLogger LOG = MnLogger.make(TranscodeProfile.class);

	private final IMediaTrackList<? extends IMediaTrack> list;
	private final IMediaItem item;
	private final Transcode transcode;
	private final MimeType mimeType;

	protected TranscodeProfile (final IMediaTrackList<? extends IMediaTrack> list, final IMediaItem item, final Transcode transcode, final MimeType mimeType) {
		if (item == null) throw new IllegalArgumentException("Item required.");
		if (transcode == null) throw new IllegalArgumentException("Transcode required.");
		if (mimeType == null) throw new IllegalArgumentException("MimeType required.");
		this.list = list;
		this.item = item;
		this.transcode = transcode;
		this.mimeType = mimeType;
	}

	public IMediaItem getItem () {
		return this.item;
	}

	public MimeType getMimeType () {
		return this.mimeType;
	}

	public String getTranscodedTitle () {
		return this.item.getTitle() + "." + this.mimeType.getExt();
	}

	public File getCacheFile () {
		return cacheFile(this.item, this.transcode, this.mimeType);
	}

	protected static File cacheFile (final IMediaItem item, final Transcode transcode, final MimeType mimeType) {
		return cacheFile(cacheFileNameWithoutExtension(item, transcode), mimeType);
	}

	protected static File cacheFile (final String nameWithoutExtension, final MimeType mimeType) {
		return new File(Config.getTranscodedDir(), nameWithoutExtension + "." + mimeType.getExt());
	}

	protected static String cacheFileNameWithoutExtension (final IMediaItem item, final Transcode transcode) {
		return ChecksumHelper.md5String(item.getFile().getAbsolutePath()) + "_" + transcode.getSymbolicName();
	}

	/**
	 * Includes leading dot.
	 */
	public String getTmpFileExt () {
		return "." + this.mimeType.getExt();
	}

	protected Long getTrimEndTimeSeconds () {
		if (this.list == null) return null;

		final String raw = findSuffesForTagStartingWith("trim_end=");
		if (StringHelper.blank(raw)) return null;

		final Long trimEnd = TimeHelper.parseDuration(raw);
		if (trimEnd != null) {
			return trimEnd;
		}

		LOG.w("Invalid trim_end: {0}", raw);
		return null;
	}

	private String findSuffesForTagStartingWith (final String startsWith) {
		List<MediaTag> tags;
		try {
			tags = this.list.getTags(this.item);
		}
		catch (MorriganException e) {
			LOG.e("Tag read failed", e);
			return null;
		}

		for (final MediaTag tag : tags) {
			if (tag.getType() == MediaTagType.MANUAL && StringHelper.startsWithIgnoreCase(tag.getTag(), startsWith)) {
				return tag.getTag().substring(startsWith.length());
			}
		}
		return null;
	}

	/**
	 * Cmd must write to cache file.
	 */
	public abstract String[] transcodeCmd (File outputFile) throws IOException;

}

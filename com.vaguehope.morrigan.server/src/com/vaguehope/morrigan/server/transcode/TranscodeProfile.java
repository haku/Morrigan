package com.vaguehope.morrigan.server.transcode;

import java.io.File;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public abstract class TranscodeProfile {

	/**
	 * Returns null if no transcode is required.
	 */
	public static TranscodeProfile forFile (final IMediaItem item, final String transcode) {
		if (Transcoder.TRANSCODE_AUDIO_ONLY.equals(transcode)) {
			if (MimeType.MP4.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				return new Mp4ToM4aTranscode(item, transcode);
			}
			else if (StringHelper.startsWithIgnoreCase(item.getMimeType(), "video")) {
				return new GenericMp3Transcode(item, transcode);
			}
			return null;
		}
		throw new IllegalArgumentException("Unsupported transcode: " + transcode);
	}

	private final IMediaItem item;
	private final String transcode;
	private final MimeType mimeType;

	protected TranscodeProfile (final IMediaItem item, final String transcode, final MimeType mimeType) {
		this.item = item;
		this.transcode = transcode;
		this.mimeType = mimeType;
	}

	public IMediaItem getItem () {
		return this.item;
	}

	public String getTranscodedTitle () {
		return this.item.getTitle() + "." + this.mimeType.getExt();
	}

	public File getCacheFile () {
		return new File(Config.getTranscodedDir(),
				ChecksumHelper.md5String(this.item.getFile().getAbsolutePath())
				+ "_" + this.transcode + "." + this.mimeType.getExt());
	}

	/**
	 * Includes leading dot.
	 */
	public String getTmpFileExt () {
		return "." + this.mimeType.getExt();
	}

	/**
	 * Cmd must write to cache file.
	 */
	public abstract String[] transcodeCmd (File outputFile);

}

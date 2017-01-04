package com.vaguehope.morrigan.player.transcode;

import java.io.File;
import java.io.IOException;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public abstract class TranscodeProfile {

	/**
	 * Returns null if no transcode is required.
	 */
	public static TranscodeProfile forFile (final IMediaItem item, final String transcode) throws IOException {
		if (Transcoder.TRANSCODE_AUDIO_ONLY.equals(transcode)) {
			if (MimeType.MP4.getMimeType().equalsIgnoreCase(item.getMimeType())) {
				return new Mp4StreamExtract(item, transcode);
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

	public MimeType getMimeType () {
		return this.mimeType;
	}

	public String getTranscodedTitle () {
		return this.item.getTitle() + "." + this.mimeType.getExt();
	}

	public File getCacheFile () {
		return cacheFile(this.item, this.transcode, this.mimeType);
	}

	protected static File cacheFile (final IMediaItem item, final String transcode, final MimeType mimeType) {
		return new File(Config.getTranscodedDir(),
				ChecksumHelper.md5String(item.getFile().getAbsolutePath())
				+ "_" + transcode + "." + mimeType.getExt());
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

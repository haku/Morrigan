package com.vaguehope.morrigan.player.transcode;

import java.io.File;
import java.io.IOException;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.MimeType;

public abstract class TranscodeProfile {

	private final IMediaItem item;
	private final Transcode transcode;
	private final MimeType mimeType;

	protected TranscodeProfile (final IMediaItem item, final Transcode transcode, final MimeType mimeType) {
		if (item == null) throw new IllegalArgumentException("Item required.");
		if (transcode == null) throw new IllegalArgumentException("Transcode required.");
		if (mimeType == null) throw new IllegalArgumentException("MimeType required.");
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

	/**
	 * Cmd must write to cache file.
	 */
	public abstract String[] transcodeCmd (File outputFile) throws IOException;

}

package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.MimeType;

public abstract class TranscodeProfile {

	private final IMediaTrack item;
	private final ItemTags tags;
	private final Transcode transcode;
	private final MimeType mimeType;

	protected TranscodeProfile (final IMediaTrack item, final ItemTags tags, final Transcode transcode, final MimeType mimeType) {
		if (item == null) throw new IllegalArgumentException("Item required.");
		if (transcode == null) throw new IllegalArgumentException("Transcode required.");
		if (mimeType == null) throw new IllegalArgumentException("MimeType required.");
		this.item = item;
		this.tags = tags;
		this.transcode = transcode;
		this.mimeType = mimeType;
	}

	public IMediaTrack getItem () {
		return this.item;
	}

	public MimeType getMimeType () {
		return this.mimeType;
	}

	public String getTranscodedTitle () {
		return this.item.getTitle() + "." + this.mimeType.getExt();
	}

	/**
	 * File may not exist or may be out of date.
	 * Always call transcodeToFile() beforehand to ensure it if fresh.
	 */
	public File getCacheFileEvenIfItDoesNotExist () {
		return cacheFile(this.item, this.transcode, this.mimeType);
	}

	public File getCacheFileIfFresh () throws MorriganException, IOException {
		final File cacheFile = getCacheFileEvenIfItDoesNotExist();
		if (!cacheFile.exists()) return null;

		final File inFile = this.item.getFile();
		if (inFile == null || !inFile.exists()) throw new IOException("Local file not found: " + inFile.getAbsolutePath());

		final long inFileLastModified = inFile.lastModified();
		if (cacheFile.lastModified() < inFileLastModified) {
			return null;
		}

		final Date newest = ConfigTag.newest(this.tags);
		if (newest != null && newest.getTime() > cacheFile.lastModified()) {
			return null;
		}

		return cacheFile;
	}

	protected static File cacheFile (final IMediaItem item, final Transcode transcode, final MimeType mimeType) {
		return cacheFile(cacheFileNameWithoutExtension(item, transcode), mimeType);
	}

	protected static File cacheFile (final String nameWithoutExtension, final MimeType mimeType) {
		return new File(Config.DEFAULT.getTranscodedDir(), nameWithoutExtension + "." + mimeType.getExt());
	}

	protected static String cacheFileNameWithoutExtension (final IMediaItem item, final Transcode transcode) {
		// TODO use better hash?
		return ChecksumHelper.md5String(item.getFile().getAbsolutePath()) + "_" + transcode.getSymbolicName();
	}

	/**
	 * Includes leading dot.
	 */
	protected String getTmpFileExt () {
		return "." + this.mimeType.getExt();
	}

	/**
	 * Stop time in seconds relative to start of file.
	 */
	protected Long getTrimEndTimeSeconds () {
		final ItemConfigTag t = ConfigTag.TRIM_END.read(this.tags);
		if (t == null) return null;
		return t.parseAsDuration();
	}

	protected String getAudioFilter() {
		final ItemConfigTag t = ConfigTag.AUDIO_FILTER.read(this.tags);
		if (t == null) return null;
		return t.parseAsString();
	}

	/**
	 * Cmd must write to cache file.
	 */
	protected abstract String[] transcodeCmd (File outputFile) throws IOException;

}

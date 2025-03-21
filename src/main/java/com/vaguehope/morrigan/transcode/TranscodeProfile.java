package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.MimeType;

public abstract class TranscodeProfile {

	protected final TranscodeContext context;
	private final MediaItem item;
	private final ItemTags tags;
	private final Transcode transcode;
	private final MimeType mimeType;

	protected TranscodeProfile (final TranscodeContext context, final MediaItem item, final ItemTags tags, final Transcode transcode, final MimeType mimeType) {
		this.context = context;
		if (item == null) throw new IllegalArgumentException("Item required.");
		if (transcode == null) throw new IllegalArgumentException("Transcode required.");
		if (mimeType == null) throw new IllegalArgumentException("MimeType required.");
		this.item = item;
		this.tags = tags;
		this.transcode = transcode;
		this.mimeType = mimeType;
	}

	public MediaItem getItem () {
		return this.item;
	}

	public MimeType getMimeType () {
		return this.mimeType;
	}

	public String getTranscodedTitle () {
		return this.item.getTitle() + "." + this.mimeType.getOutputExt();
	}

	/**
	 * File may not exist or may be out of date.
	 * Always call transcodeToFile() beforehand to ensure it if fresh.
	 */
	public File getCacheFileEvenIfItDoesNotExist () {
		return cacheFile(this.context, this.item, this.transcode, this.mimeType);
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

	protected static File cacheFile (final TranscodeContext context, final MediaItem item, final Transcode transcode, final MimeType mimeType) {
		return cacheFile(context, cacheFileNameWithoutExtension(item, transcode), mimeType);
	}

	protected static File cacheFile (final TranscodeContext context, final String nameWithoutExtension, final MimeType mimeType) {
		return new File(context.config.getTranscodedDir(), nameWithoutExtension + "." + mimeType.getOutputExt());
	}

	protected static String cacheFileNameWithoutExtension (final MediaItem item, final Transcode transcode) {
		// TODO use better hash?
		return ChecksumHelper.md5(item.getFile().getAbsolutePath()).toString(16) + "_" + transcode.getSymbolicName();
	}

	/**
	 * Includes leading dot.
	 */
	protected String getTmpFileExt () {
		return "." + this.mimeType.getOutputExt();
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

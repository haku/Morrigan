package com.vaguehope.morrigan.player.transcode;

import java.io.File;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;

public class AudioOnlyMp3Transcode extends TranscodeProfile {

	public AudioOnlyMp3Transcode (final IMediaItem item, final Transcode transcode) {
		super(item, transcode, MimeType.MP3);
	}

	@Override
	public String[] transcodeCmd (final File outputFile) {
		return new String[]{
				"ffmpeg",
				"-hide_banner",
				"-nostats",
				"-y", // Overwrite output files.
				// "-seekable", "1", // Only needed when input is HTTP and not a local file.
				"-fflags", "+genpts",
				"-threads", "0",
				"-i", getItem().getFile().getAbsolutePath(),
				"-vn",
				"-b:a", "320k",
				"-f", "mp3",
				outputFile.getAbsolutePath()};
	}

}

package com.vaguehope.morrigan.server.transcode;

import java.io.File;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class Mp4ToMp3Transcode extends TranscodeProfile {

	public Mp4ToMp3Transcode (final IMediaItem item, final String transcode) {
		super(item, transcode, MimeType.MP3);
	}

	@Override
	public String[] transcodeCmd (final File outputFile) {
		if (!StringHelper.endsWithIgnoreCase(outputFile.getName(), "." + MimeType.MP3.getExt())) {
			throw new IllegalArgumentException("Output file must end with .mp3: " + outputFile.getName());
		}

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
				"-acodec", "copy",
				"-movflags", "+faststart",
				outputFile.getAbsolutePath()};
	}

}

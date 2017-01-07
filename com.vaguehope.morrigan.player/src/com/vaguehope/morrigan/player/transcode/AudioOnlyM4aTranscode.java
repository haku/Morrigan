package com.vaguehope.morrigan.player.transcode;

import java.io.File;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class AudioOnlyM4aTranscode extends TranscodeProfile {

	public AudioOnlyM4aTranscode (final IMediaItem item, final Transcode transcode) {
		super(item, transcode, MimeType.M4A);
	}

	@Override
	public String[] transcodeCmd (final File outputFile) {
		final String ext = "." + getMimeType().getExt();
		if (!StringHelper.endsWithIgnoreCase(outputFile.getName(), ext)) {
			throw new IllegalArgumentException("Output file must end with " + ext + ": " + outputFile.getName());
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
				"-c:a", "libfdk_aac",
				"-vbr", "5", // https://trac.ffmpeg.org/wiki/Encode/AAC#fdk_vbr
				outputFile.getAbsolutePath()};
	}

}

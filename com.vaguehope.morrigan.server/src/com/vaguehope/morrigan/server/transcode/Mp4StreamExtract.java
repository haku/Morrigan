package com.vaguehope.morrigan.server.transcode;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class Mp4StreamExtract extends TranscodeProfile {

	public Mp4StreamExtract (final IMediaItem item, final String transcode) throws IOException {
		super(item, transcode, findAudioStreamType(item, transcode));
	}

	private static MimeType findAudioStreamType (final IMediaItem item, final String transcode) throws IOException {
		if (cacheFile(item, transcode, MimeType.M4A).exists()) {
			return MimeType.M4A;
		}
		else if (cacheFile(item, transcode, MimeType.MP3).exists()) {
			return MimeType.MP3;
		}

		final Set<String> codecs = Ffprobe.streamCodecs(item.getFile());
		if (codecs.contains("aac")) {
			return MimeType.M4A;
		}
		else if (codecs.contains("mp3")) {
			return MimeType.MP3;
		}

		throw new IllegalStateException("No known audio stream type: " + codecs);
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
				"-acodec", "copy",
				"-movflags", "+faststart",
				outputFile.getAbsolutePath()};
	}

}

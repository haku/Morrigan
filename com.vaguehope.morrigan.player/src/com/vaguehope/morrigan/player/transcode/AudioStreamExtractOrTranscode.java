package com.vaguehope.morrigan.player.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class AudioStreamExtractOrTranscode extends TranscodeProfile {

	public AudioStreamExtractOrTranscode (final IMediaItem item, final Transcode transcode) throws IOException {
		super(item, transcode, findAudioStreamType(item, transcode));
	}

	private static MimeType findAudioStreamType (final IMediaItem item, final Transcode transcode) throws IOException {
		final String nameWithoutExtension = cacheFileNameWithoutExtension(item, transcode);
		for (final MimeType type : new MimeType[] { MimeType.M4A, MimeType.MP3 }) {
			if (cacheFile(nameWithoutExtension, type).exists()) return type;
		}

		if (Ffprobe.streamCodecs(item.getFile()).contains("aac")) {
			return MimeType.M4A;
		}

		return MimeType.MP3;
	}

	@Override
	public String[] transcodeCmd (final File outputFile) throws IOException {
		final String ext = "." + getMimeType().getExt();
		if (!StringHelper.endsWithIgnoreCase(outputFile.getName(), ext)) {
			throw new IllegalArgumentException("Output file must end with " + ext + ": " + outputFile.getName());
		}

		final List<String> cmd = new ArrayList<String>();

		cmd.add("ffmpeg");
		cmd.add("-hide_banner");
		cmd.add("-nostats");
		cmd.add("-y"); // Overwrite output files.
		// "-seekable", "1", // Only needed when input is HTTP and not a local file.

		cmd.add("-fflags");
		cmd.add("+genpts");

		cmd.add("-threads");
		cmd.add("0");

		cmd.add("-i");
		cmd.add(getItem().getFile().getAbsolutePath());

		cmd.add("-vn");

		final Set<String> codecs = Ffprobe.streamCodecs(getItem().getFile());

		if (codecs.contains("aac")) {
			if (getMimeType() != MimeType.M4A) {
				throw new IllegalStateException("Found AAC stream, so expected mime type to be M4A, not: " + getMimeType());
			}

			cmd.add("-acodec");
			cmd.add("copy");

			cmd.add("-movflags");
			cmd.add("+faststart");
		}
		else if (codecs.contains("mp3")) {
			if (getMimeType() != MimeType.MP3) {
				throw new IllegalStateException("Found MP3 stream, so expected mime type to be MP3, not: " + getMimeType());
			}

			cmd.add("-acodec");
			cmd.add("copy");
		}
		else {
			if (getMimeType() != MimeType.MP3) {
				throw new IllegalStateException("Expected mime type to be MP3, not: " + getMimeType());
			}

			cmd.add("-b:a");
			cmd.add("320k");

			cmd.add("-f");
			cmd.add("mp3");
		}

		cmd.add(outputFile.getAbsolutePath());

		return cmd.toArray(new String[cmd.size()]);
	}

}
package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class AudioOnlyM4aTranscode extends TranscodeProfile {

	public AudioOnlyM4aTranscode (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item, final Transcode transcode) {
		super(list, item, transcode, MimeType.M4A);
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

		final FfprobeInfo info = Ffprobe.inspect(getItem().getFile());

		if (info.getCodecs().contains("aac")) {
			cmd.add("-acodec");
			cmd.add("copy");
		}
		else {
			cmd.add("-c:a");
			cmd.add("libfdk_aac");

			// https://trac.ffmpeg.org/wiki/Encode/AAC#fdk_vbr
			cmd.add("-vbr");
			cmd.add("5");
		}

		cmd.add("-movflags");
		cmd.add("+faststart");

		cmd.add(outputFile.getAbsolutePath());

		return cmd.toArray(new String[cmd.size()]);
	}

}

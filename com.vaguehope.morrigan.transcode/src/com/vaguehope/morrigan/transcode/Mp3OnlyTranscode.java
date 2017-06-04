package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class Mp3OnlyTranscode extends TranscodeProfile {

	public Mp3OnlyTranscode (final IMediaTrackList<? extends IMediaTrack> list, final IMediaItem item, final Transcode transcode) {
		super(list, item, transcode, MimeType.MP3);
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

		final Long trimEnd = getTrimEndTimeSeconds();
		if (trimEnd != null) {
			cmd.add("-ss");
			cmd.add("0");
		}

		cmd.add("-i");
		cmd.add(getItem().getFile().getAbsolutePath());

		cmd.add("-vn");

		cmd.add("-b:a");
		cmd.add("320k");

		cmd.add("-f");
		cmd.add("mp3");

		if (trimEnd != null) {
			cmd.add("-to");
			cmd.add(String.valueOf(trimEnd));
		}

		cmd.add(outputFile.getAbsolutePath());

		return cmd.toArray(new String[cmd.size()]);
	}

}

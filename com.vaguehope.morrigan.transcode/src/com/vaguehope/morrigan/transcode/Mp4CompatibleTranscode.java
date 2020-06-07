package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class Mp4CompatibleTranscode extends TranscodeProfile {

	private static final MimeType MIME_TYPE = MimeType.MP4;

	protected Mp4CompatibleTranscode (final IMediaTrack item, final ItemTags tags,
			final Transcode transcode) {
		super(item, tags, transcode, MIME_TYPE);
	}

	protected static File cacheFileMp4 (final IMediaItem item, final Transcode transcode) {
		return cacheFile(item, transcode, MIME_TYPE);
	}

	@Override
	public String[] transcodeCmd (final File outputFile) throws IOException {
		final String ext = "." + getMimeType().getExt();
		if (!StringHelper.endsWithIgnoreCase(outputFile.getName(), ext)) {
			throw new IllegalArgumentException("Output file must end with " + ext + ": " + outputFile.getName());
		}

		List<String> cmd = new ArrayList<String>();

		cmd.add("ffmpeg");
		cmd.add("-hide_banner");
		cmd.add("-nostats");
		cmd.add("-y"); // Overwrite output files.
		// "-seekable", "1", // Only needed when input is HTTP and not a local file.

		cmd.add("-fflags");
		cmd.add("+genpts");

		final Long trimEnd = getTrimEndTimeSeconds();
		final String audioFilter = getAudioFilter();

		if (trimEnd != null) {
			cmd.add("-ss");
			cmd.add("0");
		}

		cmd.add("-i");
		cmd.add(getItem().getFile().getAbsolutePath());

		final FfprobeInfo info = FfprobeCache.inspect(getItem().getFile());

		if (info.getCodecs().contains("h264") && !info.has10BitColour()) {
			cmd.add("-vcodec");
			cmd.add("copy");
		}
		else {
			// https://trac.ffmpeg.org/wiki/Encode/H.264#a1.ChooseaCRFvalue
			cmd.add("-c:v");
			cmd.add("libx264");

			cmd.add("-preset");
			// ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow.
			// Default is medium.
			cmd.add("medium");

			cmd.add("-crf");
			// 0-51 where 0 is lossless, 23 is default, and 51 is worst possible.
			cmd.add("23");
		}

		if (audioFilter == null && info.getCodecs().contains("aac")) {
			cmd.add("-acodec");
			cmd.add("copy");
		}
		else {
			cmd.add("-c:a");
			cmd.add("libfdk_aac");

			cmd.add("-vbr");
			cmd.add("5"); // https://trac.ffmpeg.org/wiki/Encode/AAC#fdk_vbr
		}

		cmd.add("-movflags");
		cmd.add("+faststart");

		if (audioFilter != null) {
			cmd.add("-filter:a");
			cmd.add(audioFilter);
		}

		if (trimEnd != null) {
			cmd.add("-to");
			cmd.add(String.valueOf(trimEnd));
		}

		cmd.add(outputFile.getAbsolutePath());

		return cmd.toArray(new String[cmd.size()]);
	}

}

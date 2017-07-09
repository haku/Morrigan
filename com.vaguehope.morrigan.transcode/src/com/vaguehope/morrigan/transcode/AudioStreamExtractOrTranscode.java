package com.vaguehope.morrigan.transcode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class AudioStreamExtractOrTranscode extends TranscodeProfile {

	private static enum Codec {
		MP3("mp3", MimeType.MP3) {
			@Override
			public void transcode (final List<String> cmd) {
				cmd.add("-b:a");
				cmd.add("320k");

				cmd.add("-f");
				cmd.add(getCodec());
			}
		},
		M4A("aac", MimeType.M4A) {
			@Override
			public void extract (final List<String> cmd) {
				cmd.add("-acodec");
				cmd.add("copy");

				cmd.add("-movflags");
				cmd.add("+faststart");

				cmd.add("-bsf:a");
				cmd.add("aac_adtstoasc");
			}

			@Override
			public void transcode (final List<String> cmd) {
				cmd.add("-c:a");
				cmd.add("libfdk_aac");

				// https://trac.ffmpeg.org/wiki/Encode/AAC#fdk_vbr
				cmd.add("-vbr");
				cmd.add("5");

				cmd.add("-movflags");
				cmd.add("+faststart");
			}
		},
		OGG("vorbis", MimeType.OGG, MimeType.OGA) {
			@Override
			public void transcode (final List<String> cmd) {
				cmd.add("-c:a");
				cmd.add("libvorbis");

				cmd.add("-qscale:a");
				cmd.add("5");
			}
		},
		FLAC("flac", MimeType.FLAC) {
			@Override
			public void transcode (final List<String> cmd) {
				cmd.add("-c:a");
				cmd.add("flac");
			}
		},
		WAV("pcm_u8", MimeType.WAV) {
			@Override
			public void transcode (final List<String> cmd) {
				cmd.add("-acodec");
				cmd.add("pcm_u8");

				cmd.add("-ar");
				cmd.add("22050");
			}
		},
		;

		private final String codec;
		private final List<MimeType> mimeTypes;

		private Codec (final String codec, final MimeType... mimeTypes) {
			this.codec = codec;
			this.mimeTypes = Collections.unmodifiableList(Arrays.asList(mimeTypes));
		}

		public String getCodec () {
			return this.codec;
		}

		public List<MimeType> getMimeTypes () {
			return this.mimeTypes;
		}

		public void extract (final List<String> cmd) {
			cmd.add("-acodec");
			cmd.add("copy");
		}

		public abstract void transcode (List<String> cmd);
	}

	private static final Map<MimeType, Codec> MIMETYPE_TO_CODEC;
	static {
		final Map<MimeType, Codec> m = new HashMap<MimeType, Codec>();
		for (final Codec c : Codec.values()) {
			for (final MimeType mt : c.getMimeTypes()) {
				m.put(mt, c);
			}
		}
		MIMETYPE_TO_CODEC = Collections.unmodifiableMap(m);
	}

	private static Codec codecForMimeType (final MimeType mimeType) {
		final Codec codec = MIMETYPE_TO_CODEC.get(mimeType);
		if (codec == null) throw new IllegalArgumentException("No codec for mime type: " + mimeType);
		return codec;
	}

	public AudioStreamExtractOrTranscode (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item,
			final Transcode transcode, final MimeType fallbackType, final MimeType... otherTypes) throws IOException {
		this(list, item, transcode, fallbackType,
				otherTypes.length > 0
				? EnumSet.copyOf(Arrays.asList(otherTypes))
				: EnumSet.noneOf(MimeType.class));
	}

	public AudioStreamExtractOrTranscode (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item,
			final Transcode transcode, final MimeType fallbackType, final Set<MimeType> otherTypes) throws IOException {
		super(list, item, transcode, findAudioStreamType(item, transcode, fallbackType, otherTypes));

		if (!MIMETYPE_TO_CODEC.containsKey(fallbackType)) throw new IllegalArgumentException("Unsupported type: " + fallbackType);
		for (final MimeType type : otherTypes) {
			if (!MIMETYPE_TO_CODEC.containsKey(type)) throw new IllegalArgumentException("Unsupported type: " + type);
		}
	}

	private static MimeType findAudioStreamType (final IMediaItem item, final Transcode transcode, final MimeType fallbackType, final Set<MimeType> otherTypes) throws IOException {
		final String nameWithoutExtension = cacheFileNameWithoutExtension(item, transcode);

		if (cacheFile(nameWithoutExtension, fallbackType).exists()) return fallbackType;
		for (final MimeType type : otherTypes) {
			if (cacheFile(nameWithoutExtension, type).exists()) return type;
		}

		if (otherTypes.size() > 0) {
			final FfprobeInfo info = Ffprobe.inspect(item.getFile());
			for (final MimeType mimeType : otherTypes) {
				final String codec = codecForMimeType(mimeType).getCodec();
				if (info.getCodecs().contains(codec)) return mimeType;
			}
		}

		return fallbackType;
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

		final FfprobeInfo info = Ffprobe.inspect(getItem().getFile());

		final Codec codec = codecForMimeType(getMimeType());
		if (info.getCodecs().contains(codec.getCodec())) {
			codec.extract(cmd);
		}
		else {
			codec.transcode(cmd);
		}

		if (trimEnd != null) {
			cmd.add("-to");
			cmd.add(String.valueOf(trimEnd));
		}

		cmd.add(outputFile.getAbsolutePath());

		return cmd.toArray(new String[cmd.size()]);
	}

}

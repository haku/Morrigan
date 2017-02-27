package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotReadVideoException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.util.StringHelper;

public final class TrackTagHelper {

	/**
	 * http://www.jthink.net/jaudiotagger/tagmapping.html
	 */
	private static final List<FieldKey> INTERESTING_TAGS = Collections.unmodifiableList(Arrays.asList(
		FieldKey.ALBUM,
		FieldKey.ALBUM_ARTIST,
		FieldKey.ARRANGER,
		FieldKey.ARTIST,
		FieldKey.ARTISTS,
		FieldKey.COMMENT,
		FieldKey.COMPOSER,
		FieldKey.CONDUCTOR,
		FieldKey.COUNTRY,
		FieldKey.DISC_NO,
		FieldKey.DISC_SUBTITLE,
		FieldKey.DJMIXER,
		FieldKey.ENCODER,
		FieldKey.ENGINEER,
		FieldKey.GENRE,
		FieldKey.KEY,
		FieldKey.LANGUAGE,
		FieldKey.LYRICIST,
		FieldKey.LYRICS,
		FieldKey.MIXER,
		FieldKey.MOOD,
		FieldKey.OCCASION,
		FieldKey.PRODUCER,
		FieldKey.RECORD_LABEL,
		FieldKey.REMIXER,
		FieldKey.SCRIPT,
		FieldKey.SUBTITLE,
		FieldKey.TAGS,
		FieldKey.TEMPO,
		FieldKey.TITLE,
		FieldKey.YEAR));

	private TrackTagHelper () {}

	public static void readTrackTags (final IMediaItemDb<?, ?> itemDb, final IMediaTrack mlt, final File file) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, MorriganException {
		final AudioFile af;
		try {
			af = AudioFileIO.read(file);
		}
		catch (final CannotReadVideoException t) {
			return;
		}
		catch (final CannotReadException t) {
			return;
		}

		final Tag tag = af.getTag();
		if (tag == null) return;

		for (final FieldKey fieldKey : INTERESTING_TAGS) {
			if (tag.hasField(fieldKey)) {
				final List<String> values = tag.getAll(fieldKey);
				final String value = longest(values);
				if (StringHelper.notBlank(value)) {
					itemDb.addTag(mlt, value, MediaTagType.AUTOMATIC, fieldKey.toString());
				}
			}
		}
	}

	private static String longest (final List<String> values) {
		String ret = null;
		for (final String val : values) {
			if (ret == null || ret.length() < val.length()) ret = val;
		}
		return ret;
	}

}

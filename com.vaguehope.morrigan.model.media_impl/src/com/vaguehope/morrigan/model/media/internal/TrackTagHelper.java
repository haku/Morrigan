package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotReadVideoException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.MediaTagType;

public final class TrackTagHelper {

	private TrackTagHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void readTrackTags (final IMediaItemDb<?, ?> itemDb, final IMediaTrack mlt, final File file) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, MorriganException {
		AudioFile af;

		try {
			af = AudioFileIO.read(file);
		}
		catch (final CannotReadVideoException t) {
			// Do nothing.
			return;
		}
		catch (final CannotReadException t) {
			// Do nothing.
			return;
		}

//		AudioHeader ah = af.getAudioHeader(); // TODO do something with this?

		final Tag tag = af.getTag();
		if (tag == null) {
			return;
		}

		final Iterator<TagField> fields;
		try {
			fields = tag.getFields();
		}
		catch (final UnsupportedOperationException t) {
			// Do nothing.
			return;
		}

		while (fields.hasNext()) {
			final TagField tagField = fields.next();
			if (!tagField.isEmpty() && !tagField.isBinary()) {

				if (tagField instanceof TagTextField) {
					final TagTextField tagTextField = (TagTextField) tagField;

					final String tagFieldString = tagTextField.getContent();
					if (tagFieldString != null && tagFieldString.length() > 0) {
						String tagId = tagTextField.getId();
						if (tagId != null && tagId.length() < 1) tagId = null;
						itemDb.addTag(mlt, tagFieldString, MediaTagType.AUTOMATIC, tagId);
					}

				}

			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

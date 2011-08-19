package net.sparktank.morrigan.model.media.internal;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import net.sparktank.morrigan.model.media.IMediaItemDb;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.MediaTagType;

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

public class TrackTagHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void readTrackTags (IMediaItemDb<?,?,?> itemDb, IMediaTrack mlt, File file) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, MorriganException {
		AudioFile af;
		
		try {
			af = AudioFileIO.read(file);
		}
		catch (CannotReadVideoException t) {
			// Do nothing.
			return;
		}
		catch (CannotReadException t) {
			// Do nothing.
			return;
		}
		
//		AudioHeader ah = af.getAudioHeader(); // TODO do something with this?
		
		Tag tag = af.getTag();
		if (tag == null) {
			return;
		}
		
		Iterator<TagField> fields;
		try {
			fields = tag.getFields();
		}
		catch (UnsupportedOperationException t) {
			// Do nothing.
			return;
		}
		
		while(fields.hasNext()) {
			TagField tagField = fields.next();
			if (!tagField.isEmpty() && !tagField.isBinary()) {
				
				if (tagField instanceof TagTextField) {
					TagTextField tagTextField = (TagTextField) tagField;
					
					String tagFieldString = tagTextField.getContent();
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

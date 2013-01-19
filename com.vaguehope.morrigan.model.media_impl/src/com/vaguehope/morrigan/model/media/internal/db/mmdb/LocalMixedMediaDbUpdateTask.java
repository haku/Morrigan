package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.factory.RecyclingFactory2;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.internal.TrackTagHelper;
import com.vaguehope.morrigan.model.media.internal.db.LocalDbUpdateTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.sqlitewrapper.DbException;

public class LocalMixedMediaDbUpdateTask extends LocalDbUpdateTask<ILocalMixedMediaDb, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.

	public static class Factory extends RecyclingFactory2<LocalMixedMediaDbUpdateTask, ILocalMixedMediaDb, RuntimeException> {

		private final PlaybackEngineFactory playbackEngineFactory;
		private final MediaFactory mediaFactory;

		public Factory (PlaybackEngineFactory playbackEngineFactory, MediaFactory mediaFactory) {
			super(false);
			this.playbackEngineFactory = playbackEngineFactory;
			this.mediaFactory = mediaFactory;
		}

		@Override
		protected boolean isValidProduct (LocalMixedMediaDbUpdateTask product) {
			return !product.isFinished();
		}

		@Override
		protected LocalMixedMediaDbUpdateTask makeNewProduct (ILocalMixedMediaDb material) {
			return new LocalMixedMediaDbUpdateTask(material, this.playbackEngineFactory, this.mediaFactory);
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlaybackEngineFactory playbackEngineFactory;
	private final MediaFactory mediaFactory;

	protected LocalMixedMediaDbUpdateTask (ILocalMixedMediaDb library, PlaybackEngineFactory playbackEngineFactory, MediaFactory mediaFactory) {
		super(library);
		this.playbackEngineFactory = playbackEngineFactory;
		this.mediaFactory = mediaFactory;
	}

	@Override
	protected ILocalMixedMediaDb getTransactional (ILocalMixedMediaDb itemList) throws DbException {
		return this.mediaFactory.getLocalMixedMediaDbTransactional(itemList);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Set<String> trackExts;
	private Set<String> pictureExts;

	@Override
	protected Set<String> getItemFileExtensions () throws MorriganException {
		this.trackExts = new HashSet<String>();
		for (String a : Config.getMediaFileTypes()) {
			this.trackExts.add(a);
		}

		this.pictureExts = new HashSet<String>();
		for (String a : Config.getPictureFileTypes()) {
			this.pictureExts.add(a);
		}

		Set<String> ret = new HashSet<String>();
		ret.addAll(this.trackExts);
		ret.addAll(this.pictureExts);
		return Collections.unmodifiableSet(ret);
	}

	@Override
	protected void mergeItems (ILocalMixedMediaDb list, IMixedMediaItem itemToKeep, IMixedMediaItem itemToBeRemove) throws MorriganException {
		list.incTrackStartCnt(itemToKeep, itemToBeRemove.getStartCount());
		list.incTrackEndCnt(itemToKeep, itemToBeRemove.getEndCount());

		if (itemToKeep.getMediaType() == MediaType.UNKNOWN && itemToBeRemove.getMediaType() != MediaType.UNKNOWN) {
			list.setItemMediaType(itemToKeep, itemToBeRemove.getMediaType());
		}

		if (itemToBeRemove.getDateAdded() != null) {
			if (itemToKeep.getDateAdded() == null
					|| itemToKeep.getDateAdded().getTime() > itemToBeRemove.getDateAdded().getTime()) {
				list.setItemDateAdded(itemToKeep, itemToBeRemove.getDateAdded());
			}
		}

		if (itemToBeRemove.getDateLastPlayed() != null) {
			if (itemToKeep.getDateLastPlayed() == null
					|| itemToKeep.getDateLastPlayed().getTime() < itemToBeRemove.getDateLastPlayed().getTime()) {
				list.setTrackDateLastPlayed(itemToKeep, itemToBeRemove.getDateLastPlayed());
			}
		}

		if (list.hasTags(itemToBeRemove)) {
			// TODO FIXME check for duplicate tags.
			list.moveTags(itemToBeRemove, itemToKeep);
		}

		if (itemToKeep.getDuration() <= 0 && itemToBeRemove.getDuration() > 0) {
			list.setTrackDuration(itemToKeep, itemToBeRemove.getDuration());
		}

		if (itemToBeRemove.isMissing() && itemToKeep.isEnabled() && !itemToBeRemove.isEnabled()) {
			list.setItemEnabled(itemToKeep, itemToBeRemove.isEnabled());
		}

		if (itemToKeep.getWidth() <= 0 && itemToKeep.getHeight() <= 0
				&& itemToBeRemove.getWidth() > 0 && itemToBeRemove.getHeight() > 0) {
			list.setPictureWidthAndHeight(itemToKeep, itemToBeRemove.getWidth(), itemToKeep.getHeight());
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private IPlaybackEngine playbackEngine = null;

	@Override
	protected boolean shouldTrackMetaData1 (TaskEventListener taskEventListener, ILocalMixedMediaDb library, IMixedMediaItem item) throws MorriganException {
		if (item.getMediaType() == MediaType.TRACK) {
			if (item.getDuration() <= 0) {
				if (!library.isMarkedAsUnreadable(item)) {
					return true;
				}
				taskEventListener.logMsg(library.getListName(), "Ignoring unreadable file '" + item.getFilepath() + "'.");
			}
			return false;
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			return item.getWidth() <= 0 || item.getHeight() <= 0;
		}
		else { // Type is unknown - determine type and call self.
			String ext = item.getFilepath();
			ext = ext.substring(ext.lastIndexOf('.') + 1).toLowerCase();

			if (this.trackExts.contains(ext)) {
				library.setItemMediaType(item, MediaType.TRACK);
				return shouldTrackMetaData1(taskEventListener, library, item);
			}
			if (this.pictureExts.contains(ext)) {
				library.setItemMediaType(item, MediaType.PICTURE);
				return shouldTrackMetaData1(taskEventListener, library, item);
			}

			taskEventListener.logMsg(library.getListName(), "Failed to determin type of file '" + item.getFilepath() + "'.");
			return false;
		}
	}

	@Override
	protected OpResult readTrackMetaData1 (ILocalMixedMediaDb list, IMixedMediaItem item, File file) {
		if (item.getMediaType() == MediaType.TRACK) {
			if (this.playbackEngine == null) {
				try {
					this.playbackEngine = this.playbackEngineFactory.newPlaybackEngine();
				}
				catch (Exception e) { // NOSONAR misconfiguration could easily cause newPlaybackEngine() to throw, so be cautious.
					return new OpResult("Failed to create playback engine instance.", e, true);
				}
			}

			try {
				int d = this.playbackEngine.readFileDuration(item.getFilepath());
				if (d > 0) list.setTrackDuration(item, d);
			}
			catch (Exception e) { // NOSONAR strange errors reading files should be reported to the user.
				return new OpResult("Error while reading metadata for '" + item.getFilepath() + "'.", e);
			}

			return null;
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			try {
				Dimension d = readImageDimensions(file);
				if (d != null && d.width > 0 && d.height > 0) {
					list.setPictureWidthAndHeight(item, d.width, d.height);
				}
			}
			catch (Exception e) { // NOSONAR strange errors reading files should be reported to the user.
				return new OpResult("Error while reading metadata for '" + item.getFilepath() + "'.", e);
			}

			return null;
		}
		else {
			return null; // Though this should never happen.
		}
	}

	@Override
	protected void cleanUpAfterTrackMetaData1 () {
		if (this.playbackEngine != null) {
			this.playbackEngine.finalise();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected void readTrackMetaData2 (ILocalMixedMediaDb list, IMixedMediaItem item, File file) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, MorriganException {
		if (item.getMediaType() == MediaType.TRACK) {
			TrackTagHelper.readTrackTags(list, item, file);
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			// TODO.
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Static helpers.

	public static Dimension readImageDimensions (File file) throws IOException {
		Dimension ret = null;

		ImageInputStream in = ImageIO.createImageInputStream(file);
		try {
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				try {
					reader.setInput(in);
					ret = new Dimension(reader.getWidth(0), reader.getHeight(0));
				}
				finally {
					reader.dispose();
				}
			}
		}
		finally {
			if (in != null) in.close();
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

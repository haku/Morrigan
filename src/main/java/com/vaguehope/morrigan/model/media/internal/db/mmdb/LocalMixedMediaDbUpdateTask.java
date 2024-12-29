package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.internal.TrackTagHelper;
import com.vaguehope.morrigan.model.media.internal.db.LocalDbUpdateTask;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.transcode.Ffprobe;
import com.vaguehope.morrigan.transcode.FfprobeCache;

public class LocalMixedMediaDbUpdateTask extends LocalDbUpdateTask<IMediaItemDb> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.

	public static class Factory extends RecyclingFactory2<LocalMixedMediaDbUpdateTask, IMediaItemDb, RuntimeException> {

		private final PlaybackEngineFactory playbackEngineFactory;
		private final MediaFactory mediaFactory;

		public Factory (final PlaybackEngineFactory playbackEngineFactory, final MediaFactory mediaFactory) {
			super(false);
			this.playbackEngineFactory = playbackEngineFactory;
			this.mediaFactory = mediaFactory;
		}

		@Override
		protected boolean isValidProduct (final LocalMixedMediaDbUpdateTask product) {
			return !product.isFinished();
		}

		@Override
		protected LocalMixedMediaDbUpdateTask makeNewProduct (final IMediaItemDb material) {
			return new LocalMixedMediaDbUpdateTask(material, this.playbackEngineFactory, this.mediaFactory);
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlaybackEngineFactory playbackEngineFactory;
	private final MediaFactory mediaFactory;

	protected LocalMixedMediaDbUpdateTask (final IMediaItemDb library, final PlaybackEngineFactory playbackEngineFactory, final MediaFactory mediaFactory) {
		super(library);
		this.playbackEngineFactory = playbackEngineFactory;
		this.mediaFactory = mediaFactory;
	}

	@Override
	protected IMediaItemDb getTransactional (final IMediaItemDb itemList) throws DbException {
		return this.mediaFactory.getLocalMixedMediaDbTransactional(itemList);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Set<String> trackExts;
	private Set<String> pictureExts;

	@Override
	protected Set<String> getItemFileExtensions () throws MorriganException {
		this.trackExts = new HashSet<>();
		for (String a : Config.getMediaFileTypes()) {
			this.trackExts.add(a);
		}

		this.pictureExts = new HashSet<>();
		for (String a : Config.getPictureFileTypes()) {
			this.pictureExts.add(a);
		}

		Set<String> ret = new HashSet<>();
		ret.addAll(this.trackExts);
		ret.addAll(this.pictureExts);
		return Collections.unmodifiableSet(ret);
	}

	@Override
	protected void mergeItems (final IMediaItemDb list, final IMediaItem itemToKeep, final IMediaItem itemToBeRemove) throws MorriganException {
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

		if (list.hasTagsIncludingDeleted(itemToBeRemove)) {
			// TODO FIXME check for duplicate tags.
			list.moveTags(itemToBeRemove, itemToKeep);
		}

		// Remove will fail if in any album.
		list.removeFromAllAlbums(itemToBeRemove);

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
	protected boolean shouldTrackMetaData1 (final TaskEventListener taskEventListener, final IMediaItemDb library, final IMediaItem item) throws MorriganException {
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
	protected OpResult readTrackMetaData1 (final IMediaItemDb list, final IMediaItem item, final File file) {
		if (item.getMediaType() == MediaType.TRACK) {
			if (this.playbackEngine == null) {
				try {
					this.playbackEngine = this.playbackEngineFactory.newPlaybackEngine();
				}
				catch (Exception e) { // NOSONAR misconfiguration could easily cause newPlaybackEngine() to throw, so be cautious.
					return new OpResult("Failed to create playback engine instance.", e, true);
				}
			}
			// NOTE playbackEngine may still be null if none could be found.

			try {
				int dSeconds = 0;
				if (Ffprobe.isAvailable()) {
					final Long dMillis = FfprobeCache.INSTANCE.inspect(item.getFile()).getDurationMillis();
					if (dMillis != null) {
						dSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(dMillis);
					}
				}

				if (dSeconds < 1 && this.playbackEngine != null) {
					dSeconds = this.playbackEngine.readFileDuration(item.getFilepath());
				}

				if (dSeconds > 0) list.setTrackDuration(item, dSeconds);
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
	protected void readTrackMetaData2 (final IMediaItemDb list, final IMediaItem item, final File file) throws IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, MorriganException {
		if (item.getMediaType() == MediaType.TRACK) {
			TrackTagHelper.readTrackTags(list, item, file);
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			// TODO.
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Static helpers.

	public static Dimension readImageDimensions (final File file) throws IOException {
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

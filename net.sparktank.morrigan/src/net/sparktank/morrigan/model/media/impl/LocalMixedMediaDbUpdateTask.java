package net.sparktank.morrigan.model.media.impl;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.LocalDbUpdateTask;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.tags.TrackTagHelper;
import net.sparktank.morrigan.model.tasks.TaskEventListener;

public class LocalMixedMediaDbUpdateTask extends LocalDbUpdateTask<LocalMixedMediaDb, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<LocalMixedMediaDbUpdateTask, LocalMixedMediaDb, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(LocalMixedMediaDbUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected LocalMixedMediaDbUpdateTask makeNewProduct(LocalMixedMediaDb material) {
			return new LocalMixedMediaDbUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	LocalMixedMediaDbUpdateTask (LocalMixedMediaDb library) {
		super(library);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Map<String, Void> ext_track;
	private Map<String, Void> ext_picture;
	
	@Override
	protected String[] getItemFileExtensions() throws MorriganException {
		String[] mediaFileTypes = Config.getMediaFileTypes();
		String[] pictureFileTypes = Config.getPictureFileTypes();
		
		this.ext_track = new HashMap<String, Void>();
		this.ext_picture = new HashMap<String, Void>();
		
		String [] ret = new String[mediaFileTypes.length + pictureFileTypes.length];
		int i = 0;
		for (String a : mediaFileTypes) {
			ret[i] = a;
			i++;
			this.ext_track.put(a,null);
		}
		for (String a : pictureFileTypes) {
			ret[i] = a;
			i++;
			this.ext_picture.put(a,null);
		}
		return ret;
	}
	
	@Override
	protected void mergeItems(IMixedMediaItem itemToKeep, IMixedMediaItem itemToBeRemove) throws MorriganException {
		this.getItemList().incTrackStartCnt(itemToKeep, itemToBeRemove.getStartCount());
		this.getItemList().incTrackEndCnt(itemToKeep, itemToBeRemove.getEndCount());
		
		if (itemToBeRemove.getDateAdded() != null) {
			if (itemToKeep.getDateAdded() == null
					|| itemToKeep.getDateAdded().getTime() > itemToBeRemove.getDateAdded().getTime()) {
				this.getItemList().setItemDateAdded(itemToKeep, itemToBeRemove.getDateAdded());
			}
		}
		
		if (itemToBeRemove.getDateLastPlayed() != null) {
			if (itemToKeep.getDateLastPlayed() == null
					|| itemToKeep.getDateLastPlayed().getTime() < itemToBeRemove.getDateLastPlayed().getTime()) {
				this.getItemList().setTrackDateLastPlayed(itemToKeep, itemToBeRemove.getDateLastPlayed());
			}
		}
		
		if (this.getItemList().hasTags(itemToBeRemove)) {
			this.getItemList().moveTags(itemToBeRemove, itemToKeep);
		}
		
		if (itemToKeep.getDuration() <= 0 && itemToBeRemove.getDuration() > 0) {
			this.getItemList().setTrackDuration(itemToKeep, itemToBeRemove.getDuration());
		}
		
		if (itemToBeRemove.isMissing() && itemToKeep.isEnabled() && !itemToBeRemove.isEnabled()) {
			this.getItemList().setItemEnabled(itemToKeep, itemToBeRemove.isEnabled());
		}
		
		if (itemToKeep.getWidth() <= 0 && itemToKeep.getHeight() <= 0
				&& itemToBeRemove.getWidth() > 0 && itemToBeRemove.getHeight() > 0) {
			this.getItemList().setPictureWidthAndHeight(itemToKeep, itemToBeRemove.getWidth(), itemToKeep.getHeight());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IPlaybackEngine playbackEngine = null;
	
	@Override
	protected boolean shouldTrackMetaData1(TaskEventListener taskEventListener, LocalMixedMediaDb library, IMixedMediaItem item) throws MorriganException {
		if (item.getMediaType() == MediaType.TRACK) {
			if (item.getDuration()<=0) {
				if (!library.isMarkedAsUnreadable(item)) {
					return true;
				}
				taskEventListener.logMsg(this.getItemList().getListName(), "Ignoring unreadable file '"+item.getFilepath()+"'.");
			}
			return false;
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			return item.getWidth() <= 0 || item.getHeight() <= 0;
		}
		else { // Type is unknown - determine type and call self.
			String ext = item.getFilepath();
			ext = ext.substring(ext.lastIndexOf(".") + 1).toLowerCase();
			
			if (this.ext_track.containsKey(ext)) {
				library.setItemMediaType(item, MediaType.TRACK);
				return shouldTrackMetaData1(taskEventListener, library, item);
			}
			if (this.ext_picture.containsKey(ext)) {
				library.setItemMediaType(item, MediaType.PICTURE);
				return shouldTrackMetaData1(taskEventListener, library, item);
			}
			
			taskEventListener.logMsg(this.getItemList().getListName(), "Failed to determin type of file '"+item.getFilepath()+"'.");
			return false;
		}
	}
	
	@Override
	protected OpResult readTrackMetaData1(LocalMixedMediaDb library, IMixedMediaItem item, File file) {
		if (item.getMediaType() == MediaType.TRACK) {
			if (this.playbackEngine == null) {
				try {
					this.playbackEngine = EngineFactory.makePlaybackEngine();
				} catch (Throwable e) {
					return new OpResult("Failed to create playback engine instance.", e, true);
				}
			}
			
			try {
				int d = this.playbackEngine.readFileDuration(item.getFilepath());
				if (d>0) this.getItemList().setTrackDuration(item, d);
			}
			catch (Throwable t) {
				// FIXME log this somewhere useful.
				return new OpResult("Error while reading metadata for '"+item.getFilepath()+"'.", t);
			}
			
			return null;
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			try {
				Dimension d = readImageDimensions(file);
				if (d != null && d.width > 0 && d.height > 0) {
					this.getItemList().setPictureWidthAndHeight(item, d.width, d.height);
				}
			}
			catch (Throwable t) {
				// FIXME log this somewhere useful.
				return new OpResult("Error while reading metadata for '"+item.getFilepath()+"'.", t);
			}
			
			return null;
		}
		else {
			return null; // Though this should never happen.
		}
	}
	
	@Override
	protected void cleanUpAfterTrackMetaData1() {
		if (this.playbackEngine != null) {
			this.playbackEngine.finalise();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void readTrackMetaData2(LocalMixedMediaDb library, IMixedMediaItem item, File file) throws Throwable {
		if (item.getMediaType() == MediaType.TRACK) {
			TrackTagHelper.readTrackTags(this.getItemList(), item, file);
		}
		else if (item.getMediaType() == MediaType.PICTURE) {
			// TODO.
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Static helpers.
	
	static public Dimension readImageDimensions(File file) throws IOException {
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

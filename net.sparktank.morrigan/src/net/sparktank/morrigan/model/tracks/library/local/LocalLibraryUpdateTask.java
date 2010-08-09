package net.sparktank.morrigan.model.tracks.library.local;

import java.io.File;

import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.LocalDbUpdateTask;
import net.sparktank.morrigan.model.tags.TrackTagHelper;
import net.sparktank.morrigan.model.tracks.MediaTrack;

public class LocalLibraryUpdateTask extends LocalDbUpdateTask<LocalMediaLibrary, MediaTrack> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<LocalLibraryUpdateTask, LocalMediaLibrary, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(LocalLibraryUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected LocalLibraryUpdateTask makeNewProduct(LocalMediaLibrary material) {
			return new LocalLibraryUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	LocalLibraryUpdateTask (LocalMediaLibrary library) {
		super(library);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Format specific methods.
	
	@Override
	protected void mergeItems(MediaTrack itemToKeep, MediaTrack itemToBeRemove) throws MorriganException {
		this.getItemList().incTrackStartCnt(itemToKeep, itemToBeRemove.getStartCount());
		this.getItemList().incTrackEndCnt(itemToKeep, itemToBeRemove.getEndCount());
		
		if (itemToBeRemove.getDateAdded() != null) {
			if (itemToKeep.getDateAdded() == null
					|| itemToKeep.getDateAdded().getTime() > itemToBeRemove.getDateAdded().getTime()) {
				this.getItemList().setDateAdded(itemToKeep, itemToBeRemove.getDateAdded());
			}
		}
		
		if (itemToBeRemove.getDateLastPlayed() != null) {
			if (itemToKeep.getDateLastPlayed() == null
					|| itemToKeep.getDateLastPlayed().getTime() < itemToBeRemove.getDateLastPlayed().getTime()) {
				this.getItemList().setDateLastPlayed(itemToKeep, itemToBeRemove.getDateLastPlayed());
			}
		}
		
		if (this.getItemList().hasTags(itemToBeRemove)) {
			this.getItemList().moveTags(itemToBeRemove, itemToKeep);
		}
		
		if (itemToKeep.getDuration() <= 0 && itemToBeRemove.getDuration() > 0) {
			this.getItemList().setTrackDuration(itemToKeep, itemToBeRemove.getDuration());
		}
		
		if (itemToBeRemove.isMissing() && itemToKeep.isEnabled() && !itemToBeRemove.isEnabled()) {
			this.getItemList().setTrackEnabled(itemToKeep, itemToBeRemove.isEnabled());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IPlaybackEngine playbackEngine = null;
	
	@Override
	protected boolean shouldTrackMetaData1(LocalMediaLibrary library, MediaTrack item) {
		return item.getDuration()<=0;
	}
	
	@Override
	protected OpResult readTrackMetaData1(LocalMediaLibrary library, MediaTrack item, File file) {
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
	
	@Override
	protected void cleanUpAfterTrackMetaData1() {
		if (this.playbackEngine != null) {
			this.playbackEngine.finalise();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void readTrackMetaData2(LocalMediaLibrary library, MediaTrack item, File file) throws Throwable {
		TrackTagHelper.readTrackTags(this.getItemList(), item, file);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.model.pictures.gallery;

import java.io.File;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.LocalDbUpdateTask;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.tasks.TaskEventListener;

public class LocalGalleryUpdateTask extends LocalDbUpdateTask<LocalGallery, IMediaPicture> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<LocalGalleryUpdateTask, LocalGallery, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(LocalGalleryUpdateTask product) {
			return !product.isFinished();
		}
		
		@Override
		protected LocalGalleryUpdateTask makeNewProduct(LocalGallery material) {
			return new LocalGalleryUpdateTask(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	LocalGalleryUpdateTask (LocalGallery library) {
		super(library);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected String[] getItemFileExtensions() throws MorriganException {
		return Config.getPictureFileTypes();
	}
	
	@Override
	protected void mergeItems(IMediaPicture itemToKeep, IMediaPicture itemToBeRemove) throws MorriganException {
		if (itemToBeRemove.getDateAdded() != null) {
			if (itemToKeep.getDateAdded() == null
					|| itemToKeep.getDateAdded().getTime() > itemToBeRemove.getDateAdded().getTime()) {
				this.getItemList().setItemDateAdded(itemToKeep, itemToBeRemove.getDateAdded());
			}
		}
		
		if (this.getItemList().hasTags(itemToBeRemove)) {
			this.getItemList().moveTags(itemToBeRemove, itemToKeep);
		}
		
		if (itemToKeep.getWidth() <= 0 && itemToKeep.getHeight() <= 0
				&& itemToBeRemove.getWidth() > 0 && itemToBeRemove.getHeight() > 0) {
			this.getItemList().setPictureWidthAndHeight(itemToKeep, itemToBeRemove.getWidth(), itemToKeep.getHeight());
		}
		
		if (itemToBeRemove.isMissing() && itemToKeep.isEnabled() && !itemToBeRemove.isEnabled()) {
			this.getItemList().setItemEnabled(itemToKeep, itemToBeRemove.isEnabled());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected boolean shouldTrackMetaData1(TaskEventListener taskEventListener, LocalGallery library, IMediaPicture item) {
		return item.getWidth() <= 0 || item.getHeight() <= 0;
	}
	
	@Override
	protected OpResult readTrackMetaData1(LocalGallery library, IMediaPicture item, File file) {
		// TODO read picture dimensions.
		
		return null;
	}
	
	@Override
	protected void cleanUpAfterTrackMetaData1() {
		 // UNUSED.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void readTrackMetaData2(LocalGallery library, IMediaPicture item, File file) throws Throwable {
//		TODO Read exif data or something?
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

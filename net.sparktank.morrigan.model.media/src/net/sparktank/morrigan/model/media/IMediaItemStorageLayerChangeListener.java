package net.sparktank.morrigan.model.media;

public interface IMediaItemStorageLayerChangeListener<T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void propertySet (String key, String value);
	
	public void mediaItemAdded (T item);
	public void mediaItemRemoved (T item);
	public void mediaItemUpdated (T item);
	
	public void mediaItemTagClassificationAdded (MediaTagClassification classification);
	public void mediaItemTagAdded (T item, MediaTag tag);
	public void mediaItemTagRemoved (T item, MediaTag tag);
	public void mediaItemTagsMoved (T fromItem, T toItem);
	public void mediaItemTagsCleared (T item);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

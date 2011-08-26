package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbItem;


public interface IMediaItemStorageLayerChangeListener<T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void eventMessage (String msg);
	
	public void propertySet (String key, String value);
	
	public void mediaItemAdded (String filePath);
	public void mediaItemsAdded (List<File> filePaths);
	public void mediaItemRemoved (String filePath);
	public void mediaItemUpdated (String filePath);
	
	public void mediaItemTagAdded (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc);
	public void mediaItemTagsMoved (IDbItem from_item, IDbItem to_item);
	public void mediaItemTagRemoved (MediaTag tag);
	public void mediaItemTagsCleared (IDbItem item);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

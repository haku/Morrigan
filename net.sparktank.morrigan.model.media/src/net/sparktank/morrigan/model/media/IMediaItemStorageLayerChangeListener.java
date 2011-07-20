package net.sparktank.morrigan.model.media;

import java.io.File;
import java.util.List;

public interface IMediaItemStorageLayerChangeListener<T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void propertySet (String key, String value);
	
	public void mediaItemAdded (String filePath);
	public void mediaItemsAdded (List<File> filePaths);
	public void mediaItemRemoved (String filePath);
	public void mediaItemUpdated (String filePath);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

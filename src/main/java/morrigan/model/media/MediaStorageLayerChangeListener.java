package morrigan.model.media;

import java.io.File;
import java.util.List;

import morrigan.model.db.IDbItem;


public interface MediaStorageLayerChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void eventMessage (String msg);

	void propertySet (String key, String value);

	void mediaItemAdded (String filePath);
	void mediaItemsAdded (List<File> filePaths);
	void mediaItemRemoved (String filePath);
	void mediaItemUpdated (MediaItem item);

	void mediaItemTagAdded (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc);
	void mediaItemTagsMoved (IDbItem fromItem, IDbItem toItem);
	void mediaItemTagRemoved (MediaTag tag);
	void mediaItemTagsCleared (IDbItem item);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

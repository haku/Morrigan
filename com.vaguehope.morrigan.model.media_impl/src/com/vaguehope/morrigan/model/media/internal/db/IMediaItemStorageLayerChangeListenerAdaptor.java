package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;

class IMediaItemStorageLayerChangeListenerAdaptor<T extends IMediaItem> implements IMediaItemStorageLayerChangeListener<T> {

	private final Collection<IMediaItemStorageLayerChangeListener<T>> changeListeners;

	public IMediaItemStorageLayerChangeListenerAdaptor (Collection<IMediaItemStorageLayerChangeListener<T>> changeListeners) {
		this.changeListeners = changeListeners;
	}

	@Override
	public void eventMessage(String msg) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.eventMessage(msg);
		}
	}

	@Override
	public void propertySet(String key, String value) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.propertySet(key, value);
		}
	}

	@Override
	public void mediaItemAdded(String filePath) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemAdded(filePath);
		}
	}

	@Override
	public void mediaItemsAdded(List<File> files) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemsAdded(files);
		}
	}

	@Override
	public void mediaItemRemoved(String filePath) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemRemoved(filePath);
		}
	}

	@Override
	public void mediaItemUpdated(String filePath) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemUpdated(filePath);
		}
	}

	@Override
	public void mediaItemTagAdded(IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemTagAdded(item, tag, type, mtc);
		}
	}

	@Override
	public void mediaItemTagsMoved(IDbItem from_item, IDbItem to_item) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemTagsMoved(from_item, to_item);
		}
	}

	@Override
	public void mediaItemTagRemoved(MediaTag tag) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemTagRemoved(tag);
		}
	}

	@Override
	public void mediaItemTagsCleared(IDbItem item) {
		for (IMediaItemStorageLayerChangeListener<T> l : this.changeListeners) {
			l.mediaItemTagsCleared(item);
		}
	}
}
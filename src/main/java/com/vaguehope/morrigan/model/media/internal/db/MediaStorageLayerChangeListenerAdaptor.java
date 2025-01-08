package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaStorageLayerChangeListener;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;

class MediaStorageLayerChangeListenerAdaptor implements MediaStorageLayerChangeListener {

	private final Collection<MediaStorageLayerChangeListener> changeListeners;

	public MediaStorageLayerChangeListenerAdaptor (final Collection<MediaStorageLayerChangeListener> changeListeners) {
		this.changeListeners = changeListeners;
	}

	@Override
	public void eventMessage (final String msg) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.eventMessage(msg);
		}
	}

	@Override
	public void propertySet (final String key, final String value) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.propertySet(key, value);
		}
	}

	@Override
	public void mediaItemAdded (final String filePath) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemAdded(filePath);
		}
	}

	@Override
	public void mediaItemsAdded (final List<File> files) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemsAdded(files);
		}
	}

	@Override
	public void mediaItemRemoved (final String filePath) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemRemoved(filePath);
		}
	}

	@Override
	public void mediaItemUpdated (final MediaItem item) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemUpdated(item);
		}
	}

	@Override
	public void mediaItemTagAdded (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemTagAdded(item, tag, type, mtc);
		}
	}

	@Override
	public void mediaItemTagsMoved (final IDbItem from_item, final IDbItem to_item) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemTagsMoved(from_item, to_item);
		}
	}

	@Override
	public void mediaItemTagRemoved (final MediaTag tag) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemTagRemoved(tag);
		}
	}

	@Override
	public void mediaItemTagsCleared (final IDbItem item) {
		for (MediaStorageLayerChangeListener l : this.changeListeners) {
			l.mediaItemTagsCleared(item);
		}
	}
}

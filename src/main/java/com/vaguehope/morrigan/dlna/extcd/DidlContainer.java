package com.vaguehope.morrigan.dlna.extcd;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jupnp.support.model.container.Container;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.EphemeralItem;
import com.vaguehope.morrigan.model.media.MediaTag;

public class DidlContainer extends EphemeralItem {

	private final String containerId;
	private final String title;

	public DidlContainer (final Container container) {
		this(container.getId(), container.getTitle());
	}

	public DidlContainer (final String containerId, final String title) {
		this.containerId = containerId;
		this.title = title;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Resource access.

	@Override
	public String getRemoteId () {
		return ContentDirectory.SEARCH_BY_ID_PREFIX + this.containerId;
	}

	@Override
	public long getFileSize () {
		return 0;
	}

	@Override
	public String getMimeType () {
		return null;
	}

	@Override
	public boolean hasRemoteLocation() {
		return false;
	}

	@Override
	public String getRemoteLocation () {
		return null;
	}

	@Override
	public String getCoverArtRemoteLocation () {
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Item metadata.

	@Override
	public String getTitle () {
		return this.title + "/";
	}

	@Override
	public boolean isPlayable () {
		return false;
	}

	@Override
	public int getDuration () {
		return 0;
	}

	@Override
	public Date getDateAdded () {
		return null;
	}

	@Override
	public MediaType getMediaType () {
		return MediaType.UNKNOWN;
	}

	@Override
	public boolean isPicture () {
		return false;
	}

	@Override
	public int getWidth () {
		return 0;
	}

	@Override
	public int getHeight () {
		return 0;
	}

	@Override
	public List<MediaTag> getTags() throws MorriganException {
		return Collections.emptyList();
	}

}

package com.vaguehope.morrigan.dlna.extcd;

import java.io.File;
import java.util.Date;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;

public class ItemWithFilepath extends EphemeralItem {

	private final IMixedMediaItem item;
	private final String filepath;
	private final File file;

	public ItemWithFilepath (final IMixedMediaItem item, final String filepath) {
		this.item = item;
		this.filepath = filepath;
		this.file = new File(filepath);
	}

	@Override
	public String getFilepath () {
		return this.filepath;
	}

	@Override
	public File getFile () {
		return this.file;
	}

	@Override
	public MediaType getMediaType () {
		return this.item.getMediaType();
	}

	@Override
	public boolean isPlayable () {
		return this.item.isPlayable();
	}

	@Override
	public int getDuration () {
		return this.item.getDuration();
	}

	@Override
	public String getCoverArtRemoteLocation () {
		return this.item.getCoverArtRemoteLocation();
	}

	@Override
	public long getFileSize () {
		return this.item.getFileSize();
	}

	@Override
	public String getMimeType () {
		return this.item.getMimeType();
	}

	@Override
	public Date getDateAdded () {
		return this.item.getDateAdded();
	}

	@Override
	public String getRemoteLocation () {
		return this.item.getRemoteLocation();
	}

	@Override
	public String getRemoteId () {
		return this.item.getRemoteId();
	}

	@Override
	public String getTitle () {
		return this.item.getTitle();
	}

	@Override
	public boolean isPicture () {
		return this.item.isPicture();
	}

	@Override
	public int getWidth () {
		return this.item.getWidth();
	}

	@Override
	public int getHeight () {
		return this.item.getHeight();
	}

}

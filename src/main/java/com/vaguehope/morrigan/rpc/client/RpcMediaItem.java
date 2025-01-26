package com.vaguehope.morrigan.rpc.client;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.morrigan.dlna.extcd.EphemeralItem;
import com.vaguehope.morrigan.dlna.extcd.Metadata;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaTag;

public class RpcMediaItem extends EphemeralItem {

	private final MediaItem rpcItem;
	private final Metadata metadata;
	private final List<MediaTag> tags;

	public RpcMediaItem(final MediaItem rpcItem, final Metadata metadata) {
		this.rpcItem = rpcItem;
		this.metadata = metadata;
		this.tags = RpcTag.convertTags(rpcItem.getTagList());
	}

	@Override
	public String getRemoteId() {
		return this.rpcItem.getId();
	}

	@Override
	public long getFileSize() {
		return this.rpcItem.getFileLength();
	}

	@Override
	public String getMimeType() {
		return this.rpcItem.getMimeType();
	}

	@Override
	public boolean hasRemoteLocation() {
		return true;
	}

	@Override
	public String getRemoteLocation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCoverArtRemoteLocation() {
		return null;
	}

	@Override
	public String getTitle() {
		return this.rpcItem.getTitle();
	}

	@Override
	public int getDuration() {
		return (int) TimeUnit.MILLISECONDS.toSeconds(this.rpcItem.getDurationMillis());
	}

	@Override
	public Date getDateAdded() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaType getMediaType() {
		if (isPlayable()) return MediaType.TRACK;
		if (isPicture()) return MediaType.PICTURE;
		return MediaType.UNKNOWN;
	}

	@Override
	public boolean isPlayable() {
		final String mt = getMimeType();
		if (mt == null) return false;
		return mt.startsWith("video/") || mt.startsWith("audio/");
	}

	@Override
	public boolean isPicture() {
		final String mt = getMimeType();
		if (mt == null) return false;
		return mt.startsWith("image/");
	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<MediaTag> getTags() throws MorriganException {
		return this.tags;
	}

	@Override
	public String toString() {
		return String.format("RpcMediaItem{%s, %s}", getRemoteId(), getTitle());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Persisted metadata.

	@Override
	public Date getDateLastPlayed () {
		return this.metadata.getDateLastPlayed();
	}

	@Override
	public long getStartCount () {
		return this.metadata.getStartCount();
	}

	@Override
	public long getEndCount () {
		return this.metadata.getEndCount();
	}

}

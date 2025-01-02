package com.vaguehope.morrigan.rpc.client;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.morrigan.dlna.extcd.EphemeralItem;
import com.vaguehope.morrigan.dlna.extcd.Metadata;

public class RpcMediaItem extends EphemeralItem {

	private final MediaItem rpcItem;
	private Function<String, String> remoteLocation;
	private final Metadata metadata;

	public RpcMediaItem(final MediaItem rpcItem, Function<String, String> remoteLocation, final Metadata metadata) {
		this.rpcItem = rpcItem;
		this.remoteLocation = remoteLocation;
		this.metadata = metadata;
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
	public String getRemoteLocation() {
		return this.remoteLocation.apply(this.rpcItem.getId());
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

package com.vaguehope.morrigan.rpc.client;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.morrigan.dlna.extcd.EphemeralItem;
import com.vaguehope.morrigan.dlna.extcd.Metadata;

public class RpcMediaItem extends EphemeralItem {

	private final String identifer;
	private final HasMediaReply hasMedia;
	private final Metadata metadata;

	public RpcMediaItem(final String identifer, final HasMediaReply hasMedia, final Metadata metadata) {
		this.identifer = identifer;
		this.hasMedia = hasMedia;
		this.metadata = metadata;
	}

	@Override
	public String getRemoteId() {
		return this.identifer;
	}

	@Override
	public long getFileSize() {
		return this.hasMedia.getItem().getFileLength();
	}

	@Override
	public String getMimeType() {
		return this.hasMedia.getItem().getMimeType();
	}

	@Override
	public String getRemoteLocation() {
		throw new UnsupportedOperationException();  // TODO figure out how to pass media to player???

		// basic idea:
		// set up entry in localhost webserver
		// return that temporary URL
	}

	@Override
	public String getCoverArtRemoteLocation() {
		return null;
	}

	@Override
	public String getTitle() {
		return this.hasMedia.getItem().getTitle();
	}
	@Override
	public int getDuration() {
		return (int) TimeUnit.MILLISECONDS.toSeconds(this.hasMedia.getItem().getDurationMillis());
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

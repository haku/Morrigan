package com.vaguehope.morrigan.dlna.players;

import java.util.concurrent.atomic.AtomicBoolean;

import org.seamless.util.MimeType;

import com.vaguehope.morrigan.dlna.util.StringHelper;
import com.vaguehope.morrigan.player.PlayItem;

class DlnaToPlay {

	private final PlayItem item;
	private final String id;
	private final String uri;
	private final MimeType mimeType;
	private final long fileSize;
	private final String coverArtUri;

	private final int durationSeconds;

	private final Runnable onStartOfTrack;
	private final Runnable onEndOfTrack;

	public DlnaToPlay (final PlayItem item, final String id, final String uri, final MimeType mimeType, final long fileSize, final int durationSeconds, final String coverArtUri, final AbstractDlnaPlayer dlnaPlayer) {
		if (item == null) throw new IllegalArgumentException();
		if (StringHelper.blank(id)) throw new IllegalArgumentException();
		if (StringHelper.blank(uri)) throw new IllegalArgumentException();
		if (mimeType == null) throw new IllegalArgumentException();
		if (durationSeconds < 1) throw new IllegalArgumentException("Can not play tracks without a known duration.");
		if (dlnaPlayer == null) throw new IllegalArgumentException();

		this.item = item;
		this.id = id;
		this.uri = uri;
		this.mimeType = mimeType;
		this.fileSize = fileSize;
		this.durationSeconds = durationSeconds;
		this.coverArtUri = coverArtUri;

		this.onStartOfTrack = new OnTrackStarted(dlnaPlayer, item);
		this.onEndOfTrack = new OnTrackComplete(dlnaPlayer, item);
	}

	public PlayItem getItem () {
		return this.item;
	}

	public String getId () {
		return this.id;
	}

	public String getUri () {
		return this.uri;
	}

	public MimeType getMimeType () {
		return this.mimeType;
	}

	public long getFileSize () {
		return this.fileSize;
	}

	public String getCoverArtUri () {
		return this.coverArtUri;
	}

	/**
	 * Will always be more than zero.
	 */
	public int getDurationSeconds () {
		return this.durationSeconds;
	}

	@Override
	public String toString () {
		return String.format("toPlay{%s, %s}", this.id, this.uri);
	}

	private final AtomicBoolean trackStarted = new AtomicBoolean(false);
	private final AtomicBoolean trackEnded = new AtomicBoolean(false);

	public boolean isStarted () {
		return this.trackStarted.get();
	}

	public void recordStartOfTrack () {
		if (this.trackStarted.compareAndSet(false, true)) this.onStartOfTrack.run();
	}

	public void recordEndOfTrack () {
		recordStartOfTrack();
		if (this.trackEnded.compareAndSet(false, true)) this.onEndOfTrack.run();
	}

}

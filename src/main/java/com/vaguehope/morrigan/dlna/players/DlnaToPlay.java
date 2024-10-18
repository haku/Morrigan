package com.vaguehope.morrigan.dlna.players;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jupnp.util.MimeType;

import com.vaguehope.morrigan.dlna.players.DlnaPlayingParamsFactory.DlnaPlayingParams;
import com.vaguehope.morrigan.dlna.util.StringHelper;
import com.vaguehope.morrigan.player.PlayItem;

class DlnaToPlay {

	private final PlayItem item;
	private final DlnaPlayingParams playingParams;

	private final Runnable onStartOfTrack;
	private final Runnable onEndOfTrack;

	public DlnaToPlay (final PlayItem item, final DlnaPlayingParams playingParams, final AbstractDlnaPlayer dlnaPlayer) {
		if (item == null) throw new IllegalArgumentException();
		if (StringHelper.blank(playingParams.id)) throw new IllegalArgumentException();
		if (StringHelper.blank(playingParams.uri)) throw new IllegalArgumentException();
		if (playingParams.mimeType == null) throw new IllegalArgumentException();
		if (playingParams.durationSeconds < 1) throw new IllegalArgumentException("Can not play tracks without a known duration.");
		if (dlnaPlayer == null) throw new IllegalArgumentException();

		this.item = item;
		this.playingParams = playingParams;

		this.onStartOfTrack = new OnTrackStarted(dlnaPlayer, item);
		this.onEndOfTrack = new OnTrackComplete(dlnaPlayer, item);
	}

	public PlayItem getItem () {
		return this.item;
	}

	public String getId () {
		return this.playingParams.id;
	}

	public String getUri () {
		return this.playingParams.uri;
	}

	public MimeType getMimeType () {
		return this.playingParams.mimeType;
	}

	public long getFileSize () {
		return this.playingParams.fileSize;
	}

	public String getCoverArtUri () {
		return this.playingParams.coverArtUri;
	}

	/**
	 * Will always be more than zero.
	 */
	public int getDurationSeconds () {
		return this.playingParams.durationSeconds;
	}

	@Override
	public String toString () {
		return String.format("toPlay{%s, %s}", this.playingParams.id, this.playingParams.uri);
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

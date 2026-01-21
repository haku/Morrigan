package morrigan.dlna.players;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jupnp.util.MimeType;

import morrigan.dlna.players.DlnaPlayingParamsFactory.DlnaPlayingParams;
import morrigan.player.PlayItem;
import morrigan.player.PlaybackRecorder;
import morrigan.util.StringHelper;

class DlnaToPlay {

	private final PlayItem item;
	private final DlnaPlayingParams playingParams;
	private final PlaybackRecorder playbackRecorder;

	public DlnaToPlay (final PlayItem item, final DlnaPlayingParams playingParams, final PlaybackRecorder playbackRecorder) {
		if (item == null) throw new IllegalArgumentException();
		if (StringHelper.blank(playingParams.id)) throw new IllegalArgumentException();
		if (StringHelper.blank(playingParams.uri)) throw new IllegalArgumentException();
		if (playingParams.mimeType == null) throw new IllegalArgumentException();
		if (playingParams.durationSeconds < 1) throw new IllegalArgumentException("Can not play tracks without a known duration.");

		this.item = item;
		this.playingParams = playingParams;
		this.playbackRecorder = playbackRecorder;
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
	private volatile long startTime = 0L;

	public boolean isStarted () {
		return this.trackStarted.get();
	}

	public void recordStartOfTrack() {
		if (this.trackStarted.compareAndSet(false, true)) {
			this.startTime = System.currentTimeMillis();
			this.playbackRecorder.recordStarted(this.item);
		}
	}

	public void recordEndOfTrack(final boolean completed) {
		if (!this.trackStarted.get()) return;

		if (this.trackEnded.compareAndSet(false, true)) {
			this.playbackRecorder.recordCompleted(this.item, completed, this.startTime);
		}
	}

}

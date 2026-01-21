package morrigan.vlc.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import morrigan.engines.playback.IPlaybackEngine;
import morrigan.engines.playback.PlaybackEngineFactory;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

public class VlcEngineFactory implements PlaybackEngineFactory {

	private static final Logger LOG = LoggerFactory.getLogger(VlcEngineFactory.class);

	private final MediaPlayerFactory factory;

	// it would be nice to specify the http timeout, but that does not seem to be configurable:
	// https://code.videolan.org/videolan/vlc/-/issues/28777
	// https://github.com/videolan/vlc/blob/master/modules/access/http.c
	private static final List<String> VLC_ARGS = Collections.unmodifiableList(Arrays.asList(new String[] {
			"--intf=dummy",
//			"--aout=alsa",
			"--no-metadata-network-access",
			"--no-video",
			"--prefetch-read-size=" + (1024 * 1024),
			"--http-reconnect",
	}));

	private final Executor executor;
	private final Consumer<MediaPlayer> prepPlayer;

	public VlcEngineFactory(final Executor executor, final boolean verbose, final List<String> extraVlcArgs) {
		this(executor, verbose, extraVlcArgs, null);
	}

	public VlcEngineFactory(final Executor executor, final boolean verbose, final List<String> extraVlcArgs, final Consumer<MediaPlayer> prepPlayer) {
		this.executor = executor;
		this.prepPlayer = prepPlayer;
		final List<String> vlcArgs = new ArrayList<>(VLC_ARGS);

		if (!verbose) {
			vlcArgs.add("--quiet");
		}

		if (extraVlcArgs != null) {
			for (final String arg : extraVlcArgs) {
				vlcArgs.add(arg);
			}
		}

		LOG.info("VLC args: {}", vlcArgs);
		this.factory = new MediaPlayerFactory(vlcArgs);
	}

	@Override
	public void dispose() {
		this.factory.release();
	}

	@Override
	public IPlaybackEngine newPlaybackEngine() {
		return new PlaybackEngine(this.factory, this.executor, this.prepPlayer);
	}

	MediaPlayerFactory getMediaPlayerFactory() {
		return this.factory;
	}

}

package com.vaguehope.morrigan.playbackimpl.vlc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

public class VlcFactory {

	private static final Logger LOG = LoggerFactory.getLogger(VlcFactory.class);

	private final MediaPlayerFactory factory;

	private static final List<String> VLC_ARGS = Collections.unmodifiableList(Arrays.asList(new String[] {
			"--intf=dummy",
//			"--aout=alsa",
			"--no-metadata-network-access",
			"--no-video",
			"--prefetch-read-size=" + (1024 * 1024),
	}));

	public VlcFactory() {
		final List<String> vlcArgs = new ArrayList<>(VLC_ARGS);
		vlcArgs.add("--quiet");
		LOG.info("VLC args: {}", vlcArgs);
		this.factory = new MediaPlayerFactory(vlcArgs);
	}

	public MediaPlayerFactory getFactory() {
		return this.factory;
	}

	public void dispose() {
		this.factory.release();
	}

}

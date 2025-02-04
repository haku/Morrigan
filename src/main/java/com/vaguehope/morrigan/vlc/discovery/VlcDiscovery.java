package com.vaguehope.morrigan.vlc.discovery;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.Args;
import com.vaguehope.morrigan.player.PlayerRegister;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.renderer.RendererDiscoverer;
import uk.co.caprica.vlcj.player.renderer.RendererDiscovererDescription;
import uk.co.caprica.vlcj.player.renderer.RendererDiscovererEventListener;
import uk.co.caprica.vlcj.player.renderer.RendererItem;

public class VlcDiscovery {

	private static final Logger LOG = LoggerFactory.getLogger(VlcDiscovery.class);

	private final boolean verbose;
	private final PlayerRegister playerRegister;
	private final ScheduledExecutorService schEx;

	public VlcDiscovery(final Args args, final PlayerRegister playerRegister, final ScheduledExecutorService schEx) {
		this.verbose = args.isVerboseLog();
		this.playerRegister = playerRegister;
		this.schEx = schEx;
	}

	public void start() {
		final MediaPlayerFactory factory = new MediaPlayerFactory();
		final List<RendererDiscovererDescription> discoverers = factory.renderers().discoverers();
		LOG.info("discoverers: {}", discoverers);
		if (discoverers.size() < 1) return;

		final RendererDiscovererDescription rdd = discoverers.get(0);
		final RendererDiscoverer dis = factory.renderers().discoverer(rdd.name());
		dis.events().addRendererDiscovererEventListener(new DisListener());
		if (!dis.start()) {
			LOG.warn("Failed to start discoverer: {}", rdd.name());
			return;
		}
		LOG.info("started: {}", rdd.name());

		if (this.verbose) {
			this.schEx.scheduleWithFixedDelay(() -> {
				LOG.info("known: {}", dis.list().rendererItems());
			}, 1, 1, TimeUnit.MINUTES);
		}
	}

	private class DisListener implements RendererDiscovererEventListener {
		@Override
		public void rendererDiscovererItemAdded(final RendererDiscoverer rendererDiscoverer, final RendererItem itemAdded) {
			LOG.info("found: {}", itemAdded);
		}

		@Override
		public void rendererDiscovererItemDeleted(final RendererDiscoverer rendererDiscoverer, final RendererItem itemDeleted) {
			LOG.info("lost: {}", itemDeleted);
		}
	}

}

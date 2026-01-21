package morrigan.vlc.discovery;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.lang.Collections;
import morrigan.Args;
import morrigan.player.Player;
import morrigan.player.PlayerRegister;
import morrigan.server.boot.ServerPlayerContainer;
import morrigan.vlc.player.VlcEngineFactory;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.renderer.RendererDiscoverer;
import uk.co.caprica.vlcj.player.renderer.RendererDiscovererDescription;
import uk.co.caprica.vlcj.player.renderer.RendererDiscovererEventListener;
import uk.co.caprica.vlcj.player.renderer.RendererItem;

public class VlcDiscovery {

	private static final Logger LOG = LoggerFactory.getLogger(VlcDiscovery.class);

	private final boolean verbose;
	private final PlayerRegister playerRegister;
	private final Executor executor;

	private final Map<String, Player> players = new ConcurrentHashMap<>();

	public VlcDiscovery(final Args args, final PlayerRegister playerRegister, final Executor executor) {
		this.executor = executor;
		this.verbose = args.isVerboseLog();
		this.playerRegister = playerRegister;
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
	}

	private void makePlayer(final RendererItem item) {
		final Consumer<MediaPlayer> prepPlayer = (p) -> p.setRenderer(item);
		final VlcEngineFactory engineFactory = new VlcEngineFactory(this.executor, this.verbose, Collections.emptyList(), prepPlayer);
		final Player player = this.playerRegister.make(makeId(item.name()), item.name(), engineFactory);

		final ServerPlayerContainer pc = new ServerPlayerContainer(item.name());
		pc.setPlayer(player);
	}

	private void removePlayer(final RendererItem item) {
		final Player player = this.players.get(item.name());
		if (player != null) {
			this.playerRegister.unregister(player);
			player.dispose();
		}
	}

	private static String makeId(String name) {
		return name.replaceAll("[^a-zA-Z0-9]", "_");
	}

	private class DisListener implements RendererDiscovererEventListener {
		@Override
		public void rendererDiscovererItemAdded(final RendererDiscoverer rendererDiscoverer, final RendererItem itemAdded) {
			LOG.info("found: {}", itemAdded);
			itemAdded.hold();
			makePlayer(itemAdded);
		}

		@Override
		public void rendererDiscovererItemDeleted(final RendererDiscoverer rendererDiscoverer, final RendererItem itemDeleted) {
			LOG.info("lost: {}", itemDeleted);
			removePlayer(itemDeleted);
			itemDeleted.release();
		}
	}

}

package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.player.internal.Player;
import com.vaguehope.morrigan.player.internal.PlayerRegister;
import com.vaguehope.morrigan.player.internal.PlayerRemote;

public class PlayerActivator implements BundleActivator {

	private static final AtomicReference<PlayerRegister> playerRegister = new AtomicReference<PlayerRegister>();

	@Override
	public void start (BundleContext context) throws Exception {
		playerRegister.set(new PlayerRegister());
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		playerRegister.getAndSet(null).dispose();
	}

	public static IPlayerLocal makeLocal (String name, IPlayerEventHandler eventHandler) {
		PlayerRegister r = getRegister();
		IPlayerLocal p = new Player(r.nextIndex(), name, eventHandler, r);
		r.register(p);
		return p;
	}

	@Deprecated
	public static IPlayerRemote makeRemote (String name, String remoteHost, int remotePlayerId) {
		PlayerRegister r = getRegister();
		PlayerRemote p = new PlayerRemote(r.nextIndex(), name, remoteHost, remotePlayerId);
		r.register(p);
		return p;
	}

	public static Collection<IPlayerAbstract> getAllPlayers () {
		return getRegister().getAll();
	}

	public static IPlayerAbstract getPlayer (int i) {
		return getRegister().get(i);
	}

	/**
	 * Returns register or throws if register not available.
	 */
	private static PlayerRegister getRegister () {
		PlayerRegister r = playerRegister.get();
		if (r == null) throw new IllegalStateException("PlayerRegister bundle is not active.");
		return r;
	}

}

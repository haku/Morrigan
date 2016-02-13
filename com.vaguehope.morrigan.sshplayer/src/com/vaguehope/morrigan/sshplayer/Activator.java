package com.vaguehope.morrigan.sshplayer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerRegister;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private CliHosts hosts;
	private Queue<Player> players;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (final BundleContext context) throws Exception {
		this.players = new LinkedList<Player>();
		this.hosts = new CliHosts();
		this.hosts.load();
		startPlayerRegisterListener(context);
	}

	@Override
	public void stop (final BundleContext context) throws Exception {
		disposePlayers();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String FILTER = "(objectclass=" + PlayerRegister.class.getName() + ")";

	private void startPlayerRegisterListener (final BundleContext context) {
		ServiceListener playerContainerSl = new ServiceListener() {
			@Override
			public void serviceChanged (final ServiceEvent ev) {
				switch (ev.getType()) {
					case ServiceEvent.REGISTERED:
						registerPlayers((PlayerRegister) context.getService(ev.getServiceReference()));
						break;
					case ServiceEvent.UNREGISTERING:
						break;
				}
			}
		};

		try {
			context.addServiceListener(playerContainerSl, FILTER);
			Collection<ServiceReference<PlayerRegister>> refs = context.getServiceReferences(PlayerRegister.class, FILTER);
			for (ServiceReference<PlayerRegister> ref : refs) {
				registerPlayers(context.getService(ref));
			}
		}
		catch (InvalidSyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	protected void registerPlayers (final PlayerRegister register) {
		for (CliHost host : this.hosts.getHosts()) {
			SshPlayer player = new SshPlayer(register.nextIndex("h"), host, register);
			register.register(player);
			this.players.add(player);
		}
	}

	public void disposePlayers () {
		Player p;
		while ((p = this.players.poll()) != null) {
			p.dispose();
		}
	}

}

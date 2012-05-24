package com.vaguehope.morrigan.sshplayer;

import java.util.Collection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.vaguehope.morrigan.player.PlayerRegister;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MplayerHosts hosts;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (BundleContext context) throws Exception {
		this.hosts = new MplayerHosts();
		this.hosts.load();
		startPlayerRegisterListener(context);
	}

	@Override
	public void stop (BundleContext context) throws Exception {/**/}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final static String FILTER = "(objectclass=" + PlayerRegister.class.getName() + ")";

	private void startPlayerRegisterListener (final BundleContext context) {
		ServiceListener playerContainerSl = new ServiceListener() {
			@Override
			public void serviceChanged (ServiceEvent ev) {
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

	protected void registerPlayers (PlayerRegister register) {
		for (MplayerHost host : this.hosts.getHosts()) {
			register.register(new SshPlayer(register.nextIndex(), host));
		}
	}

}
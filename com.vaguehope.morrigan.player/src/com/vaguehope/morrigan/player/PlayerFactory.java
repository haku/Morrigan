package com.vaguehope.morrigan.player;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class PlayerFactory {

	private PlayerFactory () {}

	/**
	 * This is not good and should be removed when I figure out how.
	 */
	public static IPlayerLocal tryMakePlayer (BundleContext context, String name, IPlayerEventHandler eventHandler) {
		ServiceReference<PlayerRegister> ref = context.getServiceReference(PlayerRegister.class);
		if (ref == null) throw new IllegalStateException("Failed to get ServiceReference for PlayerRegister.");
		PlayerRegister register = context.getService(ref);
		if (register == null) throw new IllegalStateException("Failed to get PlayerRegister service.");
		return register.makeLocal(name, eventHandler);
	}

}

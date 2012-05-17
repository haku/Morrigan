package com.vaguehope.morrigan.osgiconsole;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.player.PlayerReaderTracker;

public class Activator implements BundleActivator  {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private PlayerReaderTracker playerReaderTracker;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start(BundleContext context) throws Exception {
		this.playerReaderTracker = new PlayerReaderTracker(context);
		context.registerService(CommandProvider.class.getName(), new MorriganCommandProvider(this.playerReaderTracker), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.playerReaderTracker.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

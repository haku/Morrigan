package com.vaguehope.morrigan.osgiconsole;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.PlayerReaderTracker;
import com.vaguehope.morrigan.server.AsyncActions;

public class Activator implements BundleActivator  {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private PlayerReaderTracker playerReaderTracker;
	private MediaFactoryTracker mediaFactoryTracker;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start(BundleContext context) throws Exception {
		this.playerReaderTracker = new PlayerReaderTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);

		AsyncActions asyncActions = new AsyncActions(this.mediaFactoryTracker);
		CliHelper cliHelper = new CliHelper(this.mediaFactoryTracker);
		MorriganCommandProvider commandProvider = new MorriganCommandProvider(this.playerReaderTracker, this.mediaFactoryTracker, asyncActions, cliHelper);
		context.registerService(CommandProvider.class.getName(), commandProvider, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.mediaFactoryTracker.dispose();
		this.playerReaderTracker.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

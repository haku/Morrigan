package com.vaguehope.morrigan.osgiconsole;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.PlayerReaderTracker;
import com.vaguehope.morrigan.server.AsyncActions;
import com.vaguehope.morrigan.tasks.AsyncTasksRegisterTracker;
import com.vaguehope.morrigan.transcode.Transcoder;

public class Activator implements BundleActivator  {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private PlayerReaderTracker playerReaderTracker;
	private MediaFactoryTracker mediaFactoryTracker;
	private AsyncTasksRegisterTracker asyncTasksRegisterTracker;
	private Transcoder transcoder;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start(final BundleContext context) {
		this.playerReaderTracker = new PlayerReaderTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);
		this.asyncTasksRegisterTracker = new AsyncTasksRegisterTracker(context);
		this.transcoder = new Transcoder();

		final AsyncActions asyncActions = new AsyncActions(this.asyncTasksRegisterTracker, this.mediaFactoryTracker);
		final CliHelper cliHelper = new CliHelper(this.mediaFactoryTracker);

		context.registerService(CommandProvider.class.getName(),
				new MorriganCommandProvider(
						this.playerReaderTracker,
						this.mediaFactoryTracker,
						this.asyncTasksRegisterTracker,
						this.transcoder,
						asyncActions,
						cliHelper), null);

		context.registerService(CommandProvider.class.getName(),
				new TestingCommandProvider(cliHelper), null);
	}

	@Override
	public void stop(final BundleContext context) {
		this.transcoder.dispose();
		this.asyncTasksRegisterTracker.dispose();
		this.mediaFactoryTracker.dispose();
		this.playerReaderTracker.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package com.vaguehope.morrigan.server.boot;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.PlayerReaderTracker;
import com.vaguehope.morrigan.server.AsyncActions;
import com.vaguehope.morrigan.server.MorriganServer;
import com.vaguehope.morrigan.tasks.AsyncTasksRegisterTracker;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected static final Logger logger = Logger.getLogger(Activator.class.getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MorriganServer server;
	private UiMgr uiMgr;
	private PlayerContainer playerContainer;
	private PlayerReaderTracker playerReaderTracker;
	private MediaFactoryTracker mediaFactoryTracker;
	private AsyncTasksRegisterTracker asyncTasksRegisterTracker;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (BundleContext context) throws Exception {
		this.playerReaderTracker = new PlayerReaderTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);
		this.asyncTasksRegisterTracker = new AsyncTasksRegisterTracker(context);

		this.uiMgr = new UiMgr();
		this.playerContainer = new ServerPlayerContainer(this.uiMgr, PlaybackOrder.RANDOM);
		AsyncActions asyncActions = new AsyncActions(this.asyncTasksRegisterTracker, this.mediaFactoryTracker);
		this.server = new MorriganServer(context, this.playerReaderTracker, this.mediaFactoryTracker, this.asyncTasksRegisterTracker, asyncActions);
		this.server.start();

		context.registerService(PlayerContainer.class, this.playerContainer, null);
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		this.server.stop();
		this.server = null;
		this.playerContainer = null;
		this.uiMgr.dispose();
		this.uiMgr = null;
		this.mediaFactoryTracker.dispose();
		this.playerReaderTracker.dispose();
		this.asyncTasksRegisterTracker.dispose();
		logger.fine("Morrigan Server stopped.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

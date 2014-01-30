package com.vaguehope.morrigan.server.boot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.PlayerReaderTracker;
import com.vaguehope.morrigan.server.AsyncActions;
import com.vaguehope.morrigan.server.MorriganServer;
import com.vaguehope.morrigan.server.ServerConfig;
import com.vaguehope.morrigan.tasks.AsyncTasksRegisterTracker;
import com.vaguehope.morrigan.util.DaemonThreadFactory;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Logger logger = Logger.getLogger(Activator.class.getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MorriganServer server;
	private UiMgr uiMgr;
	private NullScreen nullScreen;
	private ServerPlayerContainer playerContainer;
	private PlayerReaderTracker playerReaderTracker;
	private MediaFactoryTracker mediaFactoryTracker;
	private AsyncTasksRegisterTracker asyncTasksRegisterTracker;
	private ExecutorService executorService;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (final BundleContext context) throws Exception {
		this.playerReaderTracker = new PlayerReaderTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);
		this.asyncTasksRegisterTracker = new AsyncTasksRegisterTracker(context);
		this.executorService = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new DaemonThreadFactory("srvboot"));

		ServerConfig config = new ServerConfig();
		this.uiMgr = new UiMgr();
		this.nullScreen = new NullScreen(this.uiMgr);
		this.playerContainer = new ServerPlayerContainer(this.uiMgr, this.nullScreen, config, this.executorService);
		AsyncActions asyncActions = new AsyncActions(this.asyncTasksRegisterTracker, this.mediaFactoryTracker);
		this.server = new MorriganServer(context, config, this.playerReaderTracker, this.mediaFactoryTracker, this.asyncTasksRegisterTracker, asyncActions);
		this.server.start();

		context.registerService(PlayerContainer.class, this.playerContainer, null);
	}

	@Override
	public void stop (final BundleContext context) throws Exception {
		this.server.stop();
		this.server = null;
		this.playerContainer.dispose();
		this.playerContainer = null;
		this.nullScreen.dispose();
		this.nullScreen = null;
		this.uiMgr.dispose();
		this.uiMgr = null;
		this.executorService.shutdownNow();
		this.executorService = null;
		this.mediaFactoryTracker.dispose();
		this.playerReaderTracker.dispose();
		this.asyncTasksRegisterTracker.dispose();
		logger.fine("Morrigan Server stopped.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package com.vaguehope.morrigan.server.boot;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.MediaFactoryTracker;
import com.vaguehope.morrigan.player.IPlayerAbstract;
import com.vaguehope.morrigan.player.IPlayerEventHandler;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
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
	private PlayerReaderTracker playerReaderTracker;
	private MediaFactoryTracker mediaFactoryTracker;
	private AsyncTasksRegisterTracker asyncTasksRegisterTracker;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (BundleContext context) throws Exception {
		this.playerReaderTracker = new PlayerReaderTracker(context);
		this.mediaFactoryTracker = new MediaFactoryTracker(context);
		this.asyncTasksRegisterTracker = new AsyncTasksRegisterTracker(context);

		AsyncActions asyncActions = new AsyncActions(this.asyncTasksRegisterTracker, this.mediaFactoryTracker);
		this.server = new MorriganServer(context, this.playerReaderTracker, this.mediaFactoryTracker, this.asyncTasksRegisterTracker, asyncActions);
		this.server.start();

		context.registerService(PlayerContainer.class, this.playerContainer, null);
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		this.server.stop();
		this.mediaFactoryTracker.dispose();
		this.playerReaderTracker.dispose();
		this.asyncTasksRegisterTracker.dispose();
		logger.fine("Morrigan Server stopped.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlayerContainer playerContainer = new PlayerContainer() {

		private IPlayerAbstract player;

		@Override
		public String getName () {
			return "Server";
		}

		@Override
		public IPlayerEventHandler getEventHandler () {
			return Activator.this.eventHandler;
		}

		@Override
		public void setPlayer (IPlayerAbstract player) {
			this.player = player;
			player.setPlaybackOrder(PlaybackOrder.RANDOM);
		}

		@Override
		public IPlayerAbstract getPlayer () {
			return this.player;
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final IPlayerEventHandler eventHandler = new IPlayerEventHandler() {

		@Override
		public void updateStatus () {
			outputStatus();
		}

		@Override
		public void asyncThrowable (Throwable t) {
			logger.log(Level.WARNING, "asyncThrowable", t);
		}

		@Override
		public Composite getCurrentMediaFrameParent () {
			return null;
		}

		@Override
		public Map<Integer, String> getMonitors () {
			return null;
		}

		@Override
		public void goFullscreen (int monitor) {/* UNUSED */}

		@Override
		public IMediaTrackList<IMediaTrack> getCurrentList () {
			return null;
		}

		@Override
		public void currentItemChanged () {/* UNUSED */}

		@Override
		public void historyChanged () {/* UNUSED */}

		@Override
		public void videoAreaSelected () {/* UNUSED */}

		@Override
		public void videoAreaClose () {/* UNUSED */}
	};

	private AtomicReference<PlayState> prevPlayState = new AtomicReference<PlayState>();

	protected void outputStatus () {
		IPlayerAbstract p = this.playerContainer.getPlayer();
		PlayState currentState = (p == null ? null : p.getPlayState());
		if (currentState != this.prevPlayState.get()) {
			this.prevPlayState.set(currentState);
			System.out.println(getPlayerStateDescription(p));
		}
	}

	private static String getPlayerStateDescription (IPlayerAbstract p) {
		if (p != null) {
			PlayState currentState = p.getPlayState();
			if (currentState != null) {
				PlayItem currentPlayItem = p.getCurrentItem();
				IMediaTrack currentItem = (currentPlayItem != null ? currentPlayItem.item : null);
				if (currentItem != null) {
					return currentState + " " + currentItem + ".";
				}
				return currentState + ".";
			}
			return "Unknown.";
		}
		return "Player unset.";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

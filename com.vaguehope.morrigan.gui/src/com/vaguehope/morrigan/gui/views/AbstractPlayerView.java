package com.vaguehope.morrigan.gui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyListener;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.dialogs.RunnableDialog;
import com.vaguehope.morrigan.gui.dialogs.jumpto.JumpToAction;
import com.vaguehope.morrigan.gui.editors.EditorFactory;
import com.vaguehope.morrigan.gui.editors.MediaItemDbEditorInput;
import com.vaguehope.morrigan.gui.editors.MediaItemListEditor;
import com.vaguehope.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.editors.mmdb.MixedMediaListEditor;
import com.vaguehope.morrigan.gui.editors.tracks.MediaTrackListEditor;
import com.vaguehope.morrigan.gui.helpers.ClipboardHelper;
import com.vaguehope.morrigan.gui.helpers.MonitorHelper;
import com.vaguehope.morrigan.gui.helpers.RefreshTimer;
import com.vaguehope.morrigan.gui.helpers.TrayHelper;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerEventHandler;
import com.vaguehope.morrigan.player.PlayerFactory;
import com.vaguehope.morrigan.screen.CoverArtProvider;
import com.vaguehope.morrigan.screen.FullscreenShell;
import com.vaguehope.morrigan.screen.ScreenPainter;
import com.vaguehope.morrigan.screen.ScreenPainter.TitleProvider;

/**
 * TODO tidy this class.
 */
public abstract class AbstractPlayerView extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	private volatile boolean isDisposed = false;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.

	@Override
	public void createPartControl (final Composite parent) {
		makeTitleProviders();
		makeHistoryRefresher();
		makeActions();
		setupHotkeys();
	}

	@Override
	public void dispose () {
		this.isDisposed = true;
		disposePlayer();
		finaliseHotkeys();
		disposeTitleProviders();
		super.dispose();
	}

	protected boolean isDisposed () {
		return this.isDisposed;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	State.

	private static final String KEY_ORDERMODE = "KEY_ORDERMODE";

	@Override
	public void init (final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		if (memento != null) {
			String modeName = memento.getString(KEY_ORDERMODE);
			if (modeName != null) {
				getPlayer().setPlaybackOrder(OrderHelper.parsePlaybackOrderByName(modeName));
			}
		}
	}

	@Override
	public void saveState (final IMemento memento) {
		memento.putString(KEY_ORDERMODE, getPlayer().getPlaybackOrder().name());

		super.saveState(memento);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Player.

	private LocalPlayer _player = null;

	public synchronized LocalPlayer getPlayer () {
		if (this._player == null) {
			this._player = PlayerFactory.tryMakePlayer(Activator.getContext(), "Gui", this.eventHandler);
		}
		return this._player;
	}

	private synchronized void disposePlayer () {
		if (this._player != null) {
			this._player.dispose();
		}
	}

	protected PlayerEventHandler getEventHandler () {
		return this.eventHandler;
	}

	private final PlayerEventHandler eventHandler = new PlayerEventHandler() {

		@Override
		public void updateStatus () {
			getSite().getShell().getDisplay().asyncExec(AbstractPlayerView.this.updateStatusRunable);
		}

		@Override
		public void historyChanged () {
			AbstractPlayerView.this.historyChangedRrefresher.run();
		}

		@Override
		public void currentItemChanged () {
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run () {
					updateTitle();
				}
			});
		}

		@Override
		public void asyncThrowable (final Throwable t) {
			System.err.println("Async Throwable: " + t.getMessage());
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(t));
		}

		@SuppressWarnings("unchecked")
		// FIXME ???
		@Override
		public IMediaTrackList<IMediaTrack> getCurrentList () {
			IMediaTrackList<IMediaTrack> ret = null;

			IEditorPart activeEditor = getViewSite().getPage().getActiveEditor();
			if (activeEditor != null && activeEditor instanceof MediaTrackListEditor<?, ?>) {
				MediaTrackListEditor<? extends IMediaTrackList<IMediaTrack>, IMediaTrack> mediaListEditor = (MediaTrackListEditor<? extends IMediaTrackList<IMediaTrack>, IMediaTrack>) activeEditor;
				IMediaTrackList<IMediaTrack> editedMediaList = mediaListEditor.getMediaList();
				ret = editedMediaList;
			}
			else if (activeEditor != null && activeEditor instanceof MixedMediaListEditor<?, ?>) {
				MixedMediaListEditor<? extends IMixedMediaList<? extends IMixedMediaItem>, ? extends IMixedMediaItem> ed = (MixedMediaListEditor<? extends IMixedMediaList<? extends IMixedMediaItem>, ? extends IMixedMediaItem>) activeEditor;
				ret = (IMediaTrackList<IMediaTrack>) ed.getMediaList();
			}

			return ret;
		}

		@Override
		public Composite getCurrentMediaFrameParent () {
			return _getCurrentMediaFrameParent();
		}

		@Override
		public Map<Integer, String> getMonitors () {
			Map<Integer, String> ret = new HashMap<Integer, String>();
			for (Entry<Integer, Monitor> e : AbstractPlayerView.this.getMonitors().entrySet()) {
				Rectangle bounds = e.getValue().getBounds();
				ret.put(e.getKey(), bounds.width + "x" + bounds.height);
			}
			return ret;
		}

		@Override
		public void goFullscreen (final int monitor) {
			FullScreenAction act = fullScreenActionFromIndex(monitor);
			goFullScreenSafe(act);
		}

		@Override
		public void videoAreaClose () {
			if (isFullScreen()) {
				removeFullScreenSafe(true);
			}
		}

		@Override
		public void videoAreaSelected () {
			if (!isFullScreen()) {
				goFullScreenSafe();
			}
			else {
				removeFullScreenSafe(true);
			}
		}

	};

	Runnable updateStatusRunable = new Runnable() {
		@Override
		public void run () {
			updateStatus();
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.

	/**
	 * This will only ever be called on the UI thread.
	 */
	protected abstract void updateStatus ();

	protected abstract void orderModeChanged (PlaybackOrder order);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	History.

	MenuManager _historyMenuMgr = new MenuManager();
	Runnable historyChangedRrefresher;

	private void makeHistoryRefresher () {
		this.historyChangedRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@Override
			public void run () {
				AbstractPlayerView.this._historyMenuMgr.removeAll();
				for (PlayItem item : getPlayer().getHistory()) {
					AbstractPlayerView.this._historyMenuMgr.add(new HistoryAction(item));
				}
			}
		});
	}

	private class HistoryAction extends Action {

		private final PlayItem item;

		public HistoryAction (final PlayItem item) {
			this.item = item;
			setText(item.item.getTitle());
		}

		@Override
		public void run () {
			getPlayer().loadAndStartPlaying(this.item.list, this.item.item);
		}

	}

	protected MenuManager getHistoryMenuMgr () {
		return this._historyMenuMgr;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue.

	protected Action showQueueAction = new Action("Queue", Activator.getImageDescriptor("icons/queue.gif")) {
		@Override
		public void run () {
			try {
				getSite().getPage().showView(ViewQueue.ID);
			}
			catch (PartInitException e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Title provider.

	private final List<ScreenPainter> _titlePainters = new ArrayList<ScreenPainter>();
	private CoverArtProvider coverArtProvider;

	private void makeTitleProviders () {
		disposeTitleProviders();
		this.coverArtProvider = new CoverArtProvider(getSite().getShell().getDisplay(), Activator.getExecutorService());
	}

	private void disposeTitleProviders () {
		if (this.coverArtProvider != null) this.coverArtProvider.dispose();
	}

	void updateTitle () {
		for (ScreenPainter sp : this._titlePainters) {
			sp.redrawAsync();
		}
	}

	public void registerScreenPainter (final ScreenPainter p) {
		if (p == null) throw new NullPointerException();
		p.setTitleProvider(this.titleProvider);
		p.setCoverArtProvider(this.coverArtProvider);
		this._titlePainters.add(p);
	}

	public void unregisterScreenPainter (final ScreenPainter p) {
		this._titlePainters.remove(p);
	}

	private final TitleProvider titleProvider = new TitleProvider() {
		@Override
		public PlayItem getItem () {
			return getPlayer().getCurrentItem();
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Video frame parent stuff.

	protected abstract void videoParentChanged (Composite newParent);

	protected abstract Composite getSecondaryVideoParent ();

	private Composite localMediaFrameParent;

	protected void setLocalMediaFrameParent (final Composite parent) {
		this.localMediaFrameParent = parent;
	}

	protected Composite getLocalMediaFrameParent () {
		if (this.localMediaFrameParent == null) throw new IllegalStateException("setMediaFrameParent() has not yet been called.");
		return this.localMediaFrameParent;
	}

	protected void updateCurrentMediaFrameParent () {
		Composite cmfp = _getCurrentMediaFrameParent();
		videoParentChanged(cmfp);

		if (getPlayer().isPlaybackEngineReady()) {
			getPlayer().setVideoFrameParent(cmfp);
		}
	}

	protected Composite _getCurrentMediaFrameParent () {
		Composite fullScreenVideoParent = getFullScreenVideoParent();
		if (fullScreenVideoParent != null) {
			return fullScreenVideoParent;
		}

		Composite secondaryVideoParent = getSecondaryVideoParent();
		if (secondaryVideoParent != null) {
			return secondaryVideoParent;
		}

		return this.localMediaFrameParent;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Full screen stuff.

	private static final long MONITOR_CACHE_MAX_AGE = 1000; // 1 second.
	private final long monitorCacheAge = 0;
	protected Map<Integer, Monitor> _monitorCacheMap = null;

	protected Map<Integer, Monitor> getMonitors () {
		if (this._monitorCacheMap == null || this.monitorCacheAge <= 0 || System.currentTimeMillis() - this.monitorCacheAge > MONITOR_CACHE_MAX_AGE) {
			getSite().getShell().getDisplay().syncExec(new Runnable() {
				@Override
				public void run () {
					Map<Integer, Monitor> ret = new LinkedHashMap<Integer, Monitor>();
					Monitor[] monitors = getSite().getShell().getDisplay().getMonitors();
					for (int i = 0; i < monitors.length; i++) {
						Integer integer = Integer.valueOf(i);
						Monitor mon = monitors[i];
						ret.put(integer, mon);
					}
					AbstractPlayerView.this._monitorCacheMap = ret;
				}
			});
		}
		return Collections.unmodifiableMap(this._monitorCacheMap);
	}

	Map<Integer, FullScreenAction> fullScreenActions = null;
	FullscreenShell fullscreenShell = null;

	/**
	 * Call from UI thread only.
	 */
	protected Collection<FullScreenAction> getFullScreenActions () {
		Map<Integer, Monitor> monitors = getMonitors();
		if (this.fullScreenActions == null || this.fullScreenActions.size() != monitors.size()) {
			LinkedHashMap<Integer, FullScreenAction> newActions = new LinkedHashMap<Integer, FullScreenAction>();
			for (Entry<Integer, Monitor> e : monitors.entrySet()) {
				newActions.put(e.getKey(), new FullScreenAction(e.getKey().intValue(), e.getValue()));
			}
			this.fullScreenActions = newActions;
		}
		return this.fullScreenActions.values();
	}

	/**
	 * Call from UI thread only.
	 */
	protected FullScreenAction fullScreenActionFromIndex (final int index) {
		getFullScreenActions();
		return this.fullScreenActions.get(Integer.valueOf(index));
	}

	boolean isFullScreen () {
		return !(this.fullscreenShell == null);
	}

	private Composite getFullScreenVideoParent () {
		if (!isFullScreen()) return null;
		return this.fullscreenShell.getShell();
	}

	void goFullScreenSafe () {
		goFullScreenSafe(null);
	}

	void goFullScreenSafe (final FullScreenAction action) {
		GoFullScreenRunner runner = new GoFullScreenRunner(action);
		if (Thread.currentThread().equals(getSite().getShell().getDisplay().getThread())) {
			runner.run();
		}
		else {
			getSite().getShell().getDisplay().asyncExec(runner);
		}
	}

	void removeFullScreenSafe (final boolean closeShell) {
		RemoveFullScreenRunner runner = new RemoveFullScreenRunner(closeShell);
		if (Thread.currentThread().equals(getSite().getShell().getDisplay().getThread())) {
			runner.run();
		}
		else {
			getSite().getShell().getDisplay().asyncExec(runner);
		}
	}

	private class GoFullScreenRunner implements Runnable {

		private final Monitor mon;
		private final Action action;

		public GoFullScreenRunner (final FullScreenAction action) {
			this.mon = action == null ? null : action.getMonitor();
			this.action = action;
		}

		@Override
		public void run () {
			if (this.mon == null || this.action == null) {
				Monitor currentMon = null;
				for (Monitor m : getMonitors().values()) {
					if (m.getBounds().contains(getSite().getShell().getDisplay().getCursorLocation())) {
						currentMon = m;
						break;
					}
				}
				if (currentMon != null) {
					for (FullScreenAction a : getFullScreenActions()) {
						if (a.getMonitor().equals(currentMon)) {
							goFullscreen(currentMon, a);
						}
					}
				}

			}
			else {
				goFullscreen(this.mon, this.action);
			}
		}

		private void goFullscreen (final Monitor m, final Action a) {
			if (isFullScreen()) {
				new RemoveFullScreenRunner(true).run();
				a.setChecked(false);
			}
			else {
				startFullScreen(m, a);
			}
		}

		private void startFullScreen (final Monitor m, final Action a) {
			Monitor refreshedMon = MonitorHelper.refreshMonitor(getSite().getShell().getDisplay(), m);
			AbstractPlayerView.this.fullscreenShell = new FullscreenShell(getSite().getShell(), refreshedMon, new Runnable() {
				@Override
				public void run () {
					removeFullScreenSafe(false);
					a.setChecked(false);
				}
			});

			registerScreenPainter(AbstractPlayerView.this.fullscreenShell.getScreenPainter());
			AbstractPlayerView.this.fullscreenShell.getShell().open();
			updateCurrentMediaFrameParent();
			a.setChecked(true);
		}

	}

	private class RemoveFullScreenRunner implements Runnable {

		private final boolean closeShell;

		/**
		 *
		 * @param closeShell this will be false if we are responding to the user having already closed the window.
		 */
		public RemoveFullScreenRunner (final boolean closeShell) {
			this.closeShell = closeShell;
		}

		@Override
		public void run () {
			if (!isFullScreen()) return;

			try {
				unregisterScreenPainter(AbstractPlayerView.this.fullscreenShell.getScreenPainter());

				if (this.closeShell) AbstractPlayerView.this.fullscreenShell.getShell().close();

				FullscreenShell fs = AbstractPlayerView.this.fullscreenShell;
				AbstractPlayerView.this.fullscreenShell = null;
				updateCurrentMediaFrameParent();

				if (fs != null && !fs.getShell().isDisposed()) {
					fs.getShell().dispose();
				}

			}
			catch (Exception e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Making actions.

	private final List<OrderSelectAction> orderMenuActions = new ArrayList<OrderSelectAction>();
	protected IAction jumpToAction;

	private void makeActions () {
		// Order menu.
		for (PlaybackOrder o : PlaybackOrder.values()) {
			OrderSelectAction a = new OrderSelectAction(o, new OrderChangedListener() {
				@Override
				public void orderChanged (final PlaybackOrder newOrder) {
					orderModeChanged(newOrder);
				}
			});
			if (getPlayer().getPlaybackOrder() == o) a.setChecked(true);
			this.orderMenuActions.add(a);
		}

		this.jumpToAction = new JumpToAction(getSite().getWorkbenchWindow());
	}

	protected List<OrderSelectAction> getOrderMenuActions () {
		return this.orderMenuActions;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Complex actions.

	public interface OrderChangedListener {
		void orderChanged (PlaybackOrder newOrder);
	}

	protected class OrderSelectAction extends Action {

		private final PlaybackOrder mode;
		private final OrderChangedListener orderChangedListener;

		public OrderSelectAction (final PlaybackOrder mode, final OrderChangedListener orderChangedListener) {
			super(mode.toString(), AS_RADIO_BUTTON);
			this.mode = mode;
			this.orderChangedListener = orderChangedListener;
		}

		@Override
		public void run () {
			if (isChecked()) {
				getPlayer().setPlaybackOrder(this.mode);
				this.orderChangedListener.orderChanged(this.mode);
			}
		}

	}

	protected class FullScreenAction extends Action {

		public static final String ID = "com.vaguehope.morrigan.gui.FullScreenAction";

		private final Monitor mon;

		public FullScreenAction (final int i, final Monitor mon) {
			super("Full screen on " + i, AS_CHECK_BOX);
			this.setId(ID);
			this.setImageDescriptor(Activator.getImageDescriptor("icons/display.gif"));
			this.mon = mon;
		}

		public Monitor getMonitor () {
			return this.mon;
		}

		@Override
		public void run () {
			goFullScreenSafe(this);
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.

	protected IAction pauseAction = new Action("Pause", Activator.getImageDescriptor("icons/pause.gif")) {
		@Override
		public void run () {
			getPlayer().pausePlaying();
		}
	};

	protected IAction stopAction = new Action("Stop", Activator.getImageDescriptor("icons/stop.gif")) {
		@Override
		public void run () {
			getPlayer().stopPlaying();
		}
	};

	protected IAction nextAction = new Action("Next", Activator.getImageDescriptor("icons/next.gif")) {
		@Override
		public void run () {
			getPlayer().nextTrack();
		}
	};

	protected IAction copyPathAction = new Action("Copy file path") {
		@Override
		public void run () {
			PlayItem currentItem = getPlayer().getCurrentItem();
			if (currentItem != null) {
				ClipboardHelper.setText(currentItem.item.getFilepath());

			}
			else {
				new MorriganMsgDlg("No track loaded desu~.").open();
			}
		}
	};

	/*
	 * TODO merge this with normal editor open code?
	 */
	protected IAction findInListAction = new Action("Find in list") {
		@Override
		public void run () {
			if (getPlayer().getCurrentItem() != null
					&& getPlayer().getCurrentItem().list != null
					&& getPlayer().getCurrentItem().item != null) {
				PlayItem currentItem = getPlayer().getCurrentItem();
				revealItemInLists(currentItem.list, currentItem.item);
			}
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Find in list.

	public void revealItemInLists (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack item) {
		try {
			if (list.getType().equals(ILocalMixedMediaDb.TYPE)) {
				MediaItemDbEditorInput input = EditorFactory.getMmdbInputBySerial(list.getSerial());
				getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, LocalMixedMediaDbEditor.ID);
			}

			IEditorPart activeEditor = getViewSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			if (activeEditor instanceof MediaItemListEditor<?, ?>) {
				MediaItemListEditor<?, ?> mediaListEditor = (MediaItemListEditor<?, ?>) activeEditor;
				mediaListEditor.revealItem(item);
			}
		}
		catch (Exception e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Hotkeys.

	private void setupHotkeys () {
		try {
			Activator.getHotkeyRegister().addHotkeyListener(this.hotkeyListener);
		}
		catch (MorriganException e) {
			this.logger.log(Level.WARNING, "Failed to add hotkey listener.", e);
		}
	}

	private void finaliseHotkeys () {
		try {
			Activator.getHotkeyRegister().removeHotkeyListener(this.hotkeyListener);
		}
		catch (MorriganException e) {
			this.logger.log(Level.WARNING, "Failed to remove hotkey listener.", e);
		}
	}

	private final IHotkeyListener hotkeyListener = new IHotkeyListener() {

		@Override
		public CanDo canDoKeyPress (final int id) {
			boolean isPlaying = false;
			boolean isPaused = false;
			if (getPlayer().isPlaybackEngineReady()) {
				PlayState playbackState = getPlayer().getPlayState();
				if (playbackState == PlayState.Playing) {
					isPlaying = true;
				}
				else if (playbackState == PlayState.Paused) {
					isPaused = true;
				}
			}

			switch (id) {

				case IHotkeyEngine.MORRIGAN_HK_SHOWHIDE:
					return CanDo.YESANDFRIENDS;

				case IHotkeyEngine.MORRIGAN_HK_STOP:
				case IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE:
					if (isPlaying) {
						return CanDo.YES;
					}
					else if (isPaused) {
						return CanDo.MAYBE;
					}
					else {
						return CanDo.NO;
					}

				case IHotkeyEngine.MORRIGAN_HK_JUMPTO:
				case IHotkeyEngine.MORRIGAN_HK_NEXT:
					if (isPlaying) {
						return CanDo.YES;
					}
					else if (isPaused || getPlayer().getCurrentList() != null) {
						return CanDo.MAYBE;
					}
					else {
						return CanDo.NO;
					}

			}

			return CanDo.NO;
		}

		@Override
		public void onKeyPress (final int id) {
			switch (id) {

				case IHotkeyEngine.MORRIGAN_HK_SHOWHIDE:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run () {
							TrayHelper.hideShowWindow(getViewSite().getWorkbenchWindow());
						}
					});
					break;

				case IHotkeyEngine.MORRIGAN_HK_JUMPTO:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run () {
							AbstractPlayerView.this.jumpToAction.run();
						}
					});
					break;

				case IHotkeyEngine.MORRIGAN_HK_STOP:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run () {
							getPlayer().stopPlaying();
						}
					});
					break;

				case IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE:
					/*
					 * Calling a JNI method in one DLL from JNI thread in a
					 * different DLL seems to cause Bad Things to happen. Call
					 * via the GUI thread just to be safe.
					 */
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run () {
							getPlayer().pausePlaying();
						}
					});
					break;

				case IHotkeyEngine.MORRIGAN_HK_NEXT:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run () {
							try {
								getPlayer().nextTrack();
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					break;

			}

		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

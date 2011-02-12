package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.JumpToDlg;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.FullscreenShell;
import net.sparktank.morrigan.gui.display.ScreenPainter;
import net.sparktank.morrigan.gui.display.ScreenPainter.TitleProvider;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.MediaItemDbEditorInput;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.MediaItemListEditorInput;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.gui.editors.mmdb.MixedMediaListEditor;
import net.sparktank.morrigan.gui.editors.tracks.MediaTrackListEditor;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.gui.engines.HotkeyRegister;
import net.sparktank.morrigan.gui.helpers.ClipboardHelper;
import net.sparktank.morrigan.gui.helpers.RefreshTimer;
import net.sparktank.morrigan.gui.helpers.TrayHelper;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaPlaylist;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackDb;
import net.sparktank.morrigan.model.media.IMediaTrackList;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaList;
import net.sparktank.morrigan.player.IPlayerEventHandler;
import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.OrderHelper;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.PlayerRegister;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * TODO tidy this class.
 */
public abstract class AbstractPlayerView extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private volatile boolean isDisposed = false;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	@Override
	public void createPartControl(Composite parent) {
		makeHistoryRefresher();
		makeActions(parent);
		setupHotkeys();
	}
	
	@Override
	public void dispose() {
		this.isDisposed = true;
		disposePlayer();
		finaliseHotkeys();
		super.dispose();
	}
	
	protected boolean isDisposed () {
		return this.isDisposed;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	State.
	
	private static final String KEY_ORDERMODE = "KEY_ORDERMODE";
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		
		if (memento != null) {
			String modeName = memento.getString(KEY_ORDERMODE);
			if (modeName != null) {
				getPlayer().setPlaybackOrder(OrderHelper.parsePlaybackOrderByName(modeName));
			}
		}
	}
	
	@Override
	public void saveState(IMemento memento) {
		memento.putString(KEY_ORDERMODE, getPlayer().getPlaybackOrder().name());
		
		super.saveState(memento);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Player.
	
	private IPlayerLocal _player = null;
	
	public synchronized IPlayerLocal getPlayer () {
		if (this._player == null) {
			this._player = PlayerRegister.makeLocalPlayer(this.eventHandler);
		}
		return this._player;
	}
	
	private void disposePlayer () {
		if (this._player != null) {
			this._player.dispose();
		}
	}
	
	protected IPlayerEventHandler getEventHandler () {
		return this.eventHandler;
	}
	
	private final IPlayerEventHandler eventHandler = new IPlayerEventHandler() {
		
		@Override
		public void updateStatus() {
			getSite().getShell().getDisplay().asyncExec(AbstractPlayerView.this.updateStatusRunable);
		}
		
		@Override
		public void historyChanged() {
			AbstractPlayerView.this.historyChangedRrefresher.run();
		}
		
		@Override
		public void currentItemChanged() {
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateTitle();
				}
			});
		}
		
		@Override
		public void asyncThrowable(Throwable t) {
			System.err.println("Async Throwable: " + t.getMessage());
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(t));
		}
		
		@SuppressWarnings("unchecked") // FIXME ???
		@Override
		public IMediaTrackList<IMediaTrack> getCurrentList() {
			IMediaTrackList<IMediaTrack> ret = null;
			
			IEditorPart activeEditor = getViewSite().getPage().getActiveEditor();
			if (activeEditor != null && activeEditor instanceof MediaTrackListEditor<?,?>) {
				MediaTrackListEditor<? extends IMediaTrackList<IMediaTrack>,IMediaTrack> mediaListEditor = (MediaTrackListEditor<? extends IMediaTrackList<IMediaTrack>, IMediaTrack>) activeEditor;
				IMediaTrackList<IMediaTrack> editedMediaList = mediaListEditor.getMediaList();
				ret = editedMediaList;
			}
			else if (activeEditor != null && activeEditor instanceof MixedMediaListEditor<?,?>) {
				MixedMediaListEditor<? extends IMixedMediaList<? extends IMixedMediaItem>, ? extends IMixedMediaItem> ed = (MixedMediaListEditor<? extends IMixedMediaList<? extends IMixedMediaItem>, ? extends IMixedMediaItem>) activeEditor;
				ret = (IMediaTrackList<IMediaTrack>) ed.getMediaList();
			}
			
			return ret;
		}
		
		@Override
		public Composite getCurrentMediaFrameParent() {
			return _getCurrentMediaFrameParent();
		}
		
		@Override
		public Map<Integer, String> getMonitors () {
			Map<Integer, String> ret = new LinkedHashMap<Integer, String>();
			for (int i = 0; i < getSite().getShell().getDisplay().getMonitors().length; i++) {
				Monitor mon = getSite().getShell().getDisplay().getMonitors()[i];
				Rectangle bounds = mon.getBounds();
				ret.put(Integer.valueOf(i), bounds.width + "x" + bounds.height);
			}
			return ret;
		}
		
		@Override
		public void videoAreaClose() {
			if (isFullScreen()) {
				removeFullScreenSafe(true);
			}
		}
		
		@Override
		public void videoAreaSelected() {
			if (!isFullScreen()) {
				goFullScreenSafe();
			} else {
				removeFullScreenSafe(true);
			}
		}
		
	};
	
	Runnable updateStatusRunable = new Runnable() {
		@Override
		public void run() {
			updateStatus();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.
	
	/**
	 * This will only ever be called on the UI thread.
	 */
	abstract protected void updateStatus ();
	
	abstract protected void orderModeChanged (PlaybackOrder order);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	History.
	
	MenuManager _historyMenuMgr = new MenuManager();
	Runnable historyChangedRrefresher;
	
	private void makeHistoryRefresher () {
		this.historyChangedRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@Override
			public void run() {
				AbstractPlayerView.this._historyMenuMgr.removeAll();
				for (PlayItem item : getPlayer().getHistory()) {
					AbstractPlayerView.this._historyMenuMgr.add(new HistoryAction(item));
				}
			}
		});
	}
	
	private class HistoryAction extends Action {
		
		private final PlayItem item;
		
		public HistoryAction (PlayItem item) {
			this.item = item;
			setText(item.item.getTitle());
		}
		
		@Override
		public void run() {
			getPlayer().loadAndStartPlaying(this.item.list, this.item.item);
		}
		
	}
	
	protected MenuManager getHistoryMenuMgr () {
		return this._historyMenuMgr;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue.
	
	protected Action showQueueAction = new Action ("Queue", Activator.getImageDescriptor("icons/queue.gif")) {
		@Override
		public void run() {
			try {
				getSite().getPage().showView(ViewQueue.ID);
			} catch (PartInitException e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Title provider.
	
	private List<ScreenPainter> _titlePainters = new ArrayList<ScreenPainter>();
	
	void updateTitle () {
		for (ScreenPainter sp : this._titlePainters){
			sp.redrawTitle();
		}
	}
	
	public void registerScreenPainter (ScreenPainter p) {
		if (p == null) throw new NullPointerException();
		
		p.setTitleProvider(this.titleProvider);
		this._titlePainters.add(p);
	}
	
	public void unregisterScreenPainter (ScreenPainter p) {
		this._titlePainters.remove(p);
	}
	
	private TitleProvider titleProvider = new TitleProvider() {
		@Override
		public PlayItem getItem () {
			return getPlayer().getCurrentItem();
		}

	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Video frame parent stuff.
	
	abstract protected void videoParentChanged (Composite newParent);
	abstract protected Composite getSecondaryVideoParent ();
	
	private Composite localMediaFrameParent;
	
	protected void setLocalMediaFrameParent (Composite parent) {
		this.localMediaFrameParent = parent;
	}
	
	protected Composite getLocalMediaFrameParent () {
		if (this.localMediaFrameParent==null) throw new IllegalStateException("setMediaFrameParent() has not yet been called.");
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
	
	FullscreenShell fullscreenShell = null;
	
	boolean isFullScreen () {
		return !(this.fullscreenShell == null);
	}
	
	private Composite getFullScreenVideoParent () {
		if (!isFullScreen()) return null;
		return this.fullscreenShell.getShell();
	}
	
	void goFullScreenSafe () {
		goFullScreenSafe(null, null);
	}
	
	void goFullScreenSafe (Monitor mon, FullScreenAction action) {
		GoFullScreenRunner runner = new GoFullScreenRunner(mon, action);
		if (Thread.currentThread().equals(getSite().getShell().getDisplay().getThread())) {
			runner.run();
		} else {
			getSite().getShell().getDisplay().asyncExec(runner);
		}
	}
	
	void removeFullScreenSafe (boolean closeShell) {
		RemoveFullScreenRunner runner = new RemoveFullScreenRunner(closeShell);
		if (Thread.currentThread().equals(getSite().getShell().getDisplay().getThread())) {
			runner.run();
		} else {
			getSite().getShell().getDisplay().asyncExec(runner);
		}
	}
	
	private class GoFullScreenRunner implements Runnable {
		
		private final Monitor mon;
		private final Action action;
		
		public GoFullScreenRunner (Monitor mon, Action action) {
			this.mon = mon;
			this.action = action;
		}
		
		@Override
		public void run() {
			if (this.mon==null || this.action == null) {
				Monitor currentMon = null;
				for (Monitor m : getSite().getShell().getDisplay().getMonitors()) {
					if (m.getBounds().contains(getSite().getShell().getDisplay().getCursorLocation())) {
						currentMon = m;
						break;
					}
				}
				if (currentMon!=null) {
					for (FullScreenAction a : AbstractPlayerView.this.fullScreenActions) {
						if (a.getMonitor().equals(currentMon)) {
							goFullscreen(currentMon, a);
						}
					}
				}
				
			} else {
				goFullscreen(this.mon, this.action);
			}
		}
		
		private void goFullscreen (Monitor m, Action a) {
			if (isFullScreen()) {
				new RemoveFullScreenRunner(true).run();
				a.setChecked(false);
			}
			else {
				startFullScreen(m, a);
			}
		}
		
		private void startFullScreen (Monitor m, final Action a) {
			AbstractPlayerView.this.fullscreenShell = new FullscreenShell(getSite().getShell(), m, new Runnable() {
				@Override
				public void run() {
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

		public RemoveFullScreenRunner (boolean closeShell) {
			this.closeShell = closeShell;
		}
		
		@Override
		public void run() {
			if (!isFullScreen()) return;
			
			try {
				unregisterScreenPainter(AbstractPlayerView.this.fullscreenShell.getScreenPainter());
				
				if (this.closeShell) AbstractPlayerView.this.fullscreenShell.getShell().close();
				
				FullscreenShell fs = AbstractPlayerView.this.fullscreenShell;
				AbstractPlayerView.this.fullscreenShell = null;
				updateCurrentMediaFrameParent();
				
				if (fs!=null && !fs.getShell().isDisposed()) {
					fs.getShell().dispose();
				}
				
			} catch (Exception e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Making actions.
	
	private List<OrderSelectAction> orderMenuActions = new ArrayList<OrderSelectAction>();
	List<FullScreenAction> fullScreenActions = new ArrayList<FullScreenAction>();
	
	private void makeActions (Composite parent) {
		// Order menu.
		for (PlaybackOrder o : PlaybackOrder.values()) {
			OrderSelectAction a = new OrderSelectAction(o, new OrderChangedListener() {
				@Override
				public void orderChanged(PlaybackOrder newOrder) {
					orderModeChanged(newOrder);
				}
			});
			if (getPlayer().getPlaybackOrder() == o) a.setChecked(true);
			this.orderMenuActions.add(a);
		}
		
		// Full screen menu.
		for (int i = 0; i < parent.getShell().getDisplay().getMonitors().length; i++) {
			Monitor mon = parent.getShell().getDisplay().getMonitors()[i];
			FullScreenAction a = new FullScreenAction(i, mon);
			this.fullScreenActions.add(a);
		}
	}
	
	protected List<OrderSelectAction> getOrderMenuActions () {
		return this.orderMenuActions;
	}
	
	protected List<FullScreenAction> getFullScreenActions () {
		return this.fullScreenActions;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Complex actions.
	
	public interface OrderChangedListener {
		public void orderChanged (PlaybackOrder newOrder);
	}
	
	protected class OrderSelectAction extends Action {
		
		private final PlaybackOrder mode;
		private final OrderChangedListener orderChangedListener;
		
		public OrderSelectAction (PlaybackOrder mode, OrderChangedListener orderChangedListener) {
			super(mode.toString(), AS_RADIO_BUTTON);
			this.mode = mode;
			this.orderChangedListener = orderChangedListener;
		}
		
		@Override
		public void run() {
			if (isChecked()) {
				getPlayer().setPlaybackOrder(this.mode);
				this.orderChangedListener.orderChanged(this.mode);
			}
		}
		
	}
	
	protected class FullScreenAction extends Action {
		
		private final Monitor mon;
		
		public FullScreenAction (int i, Monitor mon) {
			super("Full screen on " + i, AS_CHECK_BOX);
			this.setImageDescriptor(Activator.getImageDescriptor("icons/display.gif"));
			this.mon = mon;
		}
		
		public Monitor getMonitor () {
			return this.mon;
		}
		
		@Override
		public void run() {
			goFullScreenSafe(this.mon, this);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	protected IAction pauseAction = new Action("Pause", Activator.getImageDescriptor("icons/pause.gif")) {
		@Override
		public void run() {
			getPlayer().pausePlaying();
		}
	};
	
	protected IAction stopAction = new Action("Stop", Activator.getImageDescriptor("icons/stop.gif")) {
		@Override
		public void run() {
			getPlayer().stopPlaying();
		}
	};
	
	protected IAction nextAction = new Action("Next", Activator.getImageDescriptor("icons/next.gif")) {
		@Override
		public void run() {
			getPlayer().nextTrack();
		}
	};
	
	protected IAction copyPathAction = new Action("Copy file path") {
		@Override
		public void run() {
			PlayItem currentItem = getPlayer().getCurrentItem();
			if (currentItem != null) {
				ClipboardHelper.setText(currentItem.item.getFilepath(), Display.getCurrent());
			
			} else {
				new MorriganMsgDlg("No track loaded desu~.").open();
			}
		}
	};
	
	/*
	 * TODO merge this with normal editor open code?
	 */
	protected IAction findInListAction = new Action("Find in list") {
		@Override
		public void run() {
			if (getPlayer().getCurrentItem() != null
					&& getPlayer().getCurrentItem().list != null
					&& getPlayer().getCurrentItem().item != null) {
				PlayItem currentItem = getPlayer().getCurrentItem();
				revealItemInLists(currentItem.list, currentItem.item);
			}
		}
	};
	
	protected IAction jumpToAction = new Action ("Jump to...") {
		@Override
		public void run() {
			IMediaTrackList<? extends IMediaTrack> currentList = getPlayer().getCurrentList();
			if (currentList == null) return;
			if (!(currentList instanceof IMediaTrackDb<?,?,?>)) return;
			
			JumpToDlg dlg = new JumpToDlg(getViewSite().getShell(), (IMediaTrackDb<?,?,?>) currentList);
			PlayItem item = dlg.open();
			if (item != null) {
				if ((dlg.getKeyMask() & SWT.SHIFT) != 0 && (dlg.getKeyMask() & SWT.CONTROL) != 0) {
					revealItemInLists(currentList, item.item);
				}
				else if ((dlg.getKeyMask() & SWT.SHIFT) != 0 || (dlg.getKeyMask() & SWT.CONTROL) != 0) {
					getPlayer().addToQueue(item);
				} else {
					getPlayer().loadAndStartPlaying(item);
				}
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Find in list.
	
	protected void revealItemInLists (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack item) {
		try {
			if (list.getType().equals(ILocalMixedMediaDb.TYPE)) {
				MediaItemDbEditorInput input = EditorFactory.getMmdbInput(list.getListId());
				getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, LocalMixedMediaDbEditor.ID);
			}
			else if (list.getType().equals(IMediaPlaylist.TYPE)) {
				MediaItemListEditorInput<IMediaPlaylist> input = EditorFactory.getMediaPlaylistInput(list.getListId());
				getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, PlaylistEditor.ID);
			}
			
			IEditorPart activeEditor = getViewSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			if (activeEditor instanceof MediaItemListEditor<?,?>) {
				MediaItemListEditor<?,?> mediaListEditor = (MediaItemListEditor<?,?>) activeEditor;
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
			HotkeyRegister.addHotkeyListener(this.hotkeyListener);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private void finaliseHotkeys () {
		try {
			HotkeyRegister.removeHotkeyListener(this.hotkeyListener);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private IHotkeyListener hotkeyListener = new IHotkeyListener() {
		
		@Override
		public CanDo canDoKeyPress(int id) {
			boolean isPlaying = false;
			boolean isPaused = false;
			if (getPlayer().isPlaybackEngineReady()) {
				PlayState playbackState = getPlayer().getPlayState();
				if (playbackState == PlayState.Playing) {
					isPlaying = true;
				}else if (playbackState == PlayState.Paused) {
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
					} else if (isPaused) {
						return CanDo.MAYBE;
					} else {
						return CanDo.NO;
					}
					
				case IHotkeyEngine.MORRIGAN_HK_JUMPTO:
				case IHotkeyEngine.MORRIGAN_HK_NEXT:
					if (isPlaying) {
						return CanDo.YES;
					} else if (isPaused || getPlayer().getCurrentList() != null) {
						return CanDo.MAYBE;
					} else {
						return CanDo.NO;
					}
				
			}
			
			return CanDo.NO;
		}
		
		@Override
		public void onKeyPress(int id) {
			switch (id) {
				
				case IHotkeyEngine.MORRIGAN_HK_SHOWHIDE:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							TrayHelper.hideShowWindow(getViewSite().getWorkbenchWindow());
						}
					});
					break;
					
				case IHotkeyEngine.MORRIGAN_HK_JUMPTO:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							AbstractPlayerView.this.jumpToAction.run();
						}
					});
					break;
				
				case IHotkeyEngine.MORRIGAN_HK_STOP:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							getPlayer().stopPlaying();
						}
					});
					break;
					
				case IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE:
					/* Calling a JNI method in one DLL
					 * from JNI thread in a different DLL
					 * seems to cause Bad Things to happen.
					 * Call via the GUI thread just to be safe.
					 */
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							getPlayer().pausePlaying();
						}
					});
					break;
					
				case IHotkeyEngine.MORRIGAN_HK_NEXT:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								getPlayer().nextTrack();
							} catch (Exception e) {
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

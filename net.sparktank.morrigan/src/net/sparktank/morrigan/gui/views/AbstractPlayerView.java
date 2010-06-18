package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.engines.HotkeyRegister;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.JumpToDlg;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.FullscreenShell;
import net.sparktank.morrigan.gui.display.ScreenPainter;
import net.sparktank.morrigan.gui.display.TrayHelper;
import net.sparktank.morrigan.gui.display.ScreenPainter.TitleProvider;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.LibraryEditorInput;
import net.sparktank.morrigan.gui.editors.LocalLibraryEditor;
import net.sparktank.morrigan.gui.editors.MediaTrackListEditor;
import net.sparktank.morrigan.gui.editors.MediaTrackListEditorInput;
import net.sparktank.morrigan.gui.editors.PlaylistEditor;
import net.sparktank.morrigan.gui.helpers.ClipboardHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaItemList;
import net.sparktank.morrigan.model.MediaTrack;
import net.sparktank.morrigan.model.MediaTrackList;
import net.sparktank.morrigan.model.library.local.LocalMediaLibrary;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.playlist.PlayItem;
import net.sparktank.morrigan.player.IPlayerEventHandler;
import net.sparktank.morrigan.player.OrderHelper;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
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
		makeActions(parent);
		setupHotkeys();
	}
	
	@Override
	public void dispose() {
		isDisposed = true;
		disposePlayer();
		finaliseHotkeys();
		super.dispose();
	}
	
	protected boolean isDisposed () {
		return isDisposed;
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
	
	private Player _player = null;
	
	public synchronized Player getPlayer () {
		if (_player == null) {
			_player = PlayerRegister.makePlayer(eventHandler);
		}
		return _player;
	}
	
	private void disposePlayer () {
		if (_player != null) {
			_player.dispose();
		}
	}
	
	protected IPlayerEventHandler getEventHandler () {
		return eventHandler;
	}
	
	private final IPlayerEventHandler eventHandler = new IPlayerEventHandler() {
		
		public boolean doOnForegroudThread(Runnable r) {
			getSite().getShell().getDisplay().asyncExec(r);
			return true;
		};
		
		@Override
		public void updateStatus() {
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
		}
		
		@Override
		public void historyChanged() {
			callRefreshHistoryMenuMgr();
		}
		
		public void currentItemChanged() {
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateTitle();
				}
			});
		}
		
		public void asyncThrowable(Throwable t) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(t));
		};
		
		@SuppressWarnings("unchecked") // FIXME ???
		@Override
		public MediaTrackList<MediaTrack> getCurrentList() {
			MediaTrackList<MediaTrack> ret = null;
			
			IEditorPart activeEditor = getViewSite().getPage().getActiveEditor();
			if (activeEditor != null && activeEditor instanceof MediaTrackListEditor<?,?>) {
				MediaTrackListEditor<? extends MediaTrackList<MediaTrack>,MediaTrack> mediaListEditor = (MediaTrackListEditor<? extends MediaItemList<MediaTrack>,MediaTrack>) activeEditor;
				MediaTrackList<MediaTrack> editedMediaList = mediaListEditor.getMediaList();
				ret = editedMediaList;
			}
			return ret;
		}
		
		@Override
		public Composite getCurrentMediaFrameParent() {
			return _getCurrentMediaFrameParent();
		}
		
		public void videoAreaClose() {
			if (isFullScreen()) {
				removeFullScreenSafe(true);
			}
		};
		
		@Override
		public void videoAreaSelected() {
			if (!isFullScreen()) {
				goFullScreenSafe();
			} else {
				removeFullScreenSafe(true);
			}
		}
		
	};
	
	private Runnable updateStatusRunable = new Runnable() {
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
	
	private MenuManager _historyMenuMgr = new MenuManager();
	
	private volatile boolean refreshHistoryMenuMgrScheduled = false;
	
	private void callRefreshHistoryMenuMgr () {
		synchronized (refreshHistoryMenuMgr) {
			if (!refreshHistoryMenuMgrScheduled) {
				refreshHistoryMenuMgrScheduled = true;
				getSite().getShell().getDisplay().asyncExec(refreshHistoryMenuMgr);
			}
		}
	}
	
	private Runnable refreshHistoryMenuMgr = new Runnable() {
		@Override
		public void run() {
			synchronized (refreshHistoryMenuMgr) {
				_historyMenuMgr.removeAll();
				for (PlayItem item : getPlayer().getHistory()) {
					_historyMenuMgr.add(new HistoryAction(item));
				}
				refreshHistoryMenuMgrScheduled = false;
			}
		}
	};
	
	private class HistoryAction extends Action {
		
		private final PlayItem item;
		
		public HistoryAction (PlayItem item) {
			this.item = item;
			setText(item.item.getTitle());
		}
		
		@Override
		public void run() {
			getPlayer().loadAndStartPlaying(item.list, item.item);
		}
		
	}
	
	protected MenuManager getHistoryMenuMgr () {
		return _historyMenuMgr;
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
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Title provider.
	
	private List<ScreenPainter> _titlePainters = new ArrayList<ScreenPainter>();
	
	private void updateTitle () {
		for (ScreenPainter sp : _titlePainters){
			sp.redrawTitle();
		}
	}
	
	public void registerScreenPainter (ScreenPainter p) {
		if (p == null) throw new NullPointerException();
		
		p.setTitleProvider(titleProvider);
		_titlePainters.add(p);
	}
	
	public void unregisterScreenPainter (ScreenPainter p) {
		_titlePainters.remove(p);
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
		localMediaFrameParent = parent;
	}
	
	protected Composite getLocalMediaFrameParent () {
		if (localMediaFrameParent==null) throw new IllegalStateException("setMediaFrameParent() has not yet been called.");
		return localMediaFrameParent;
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
			
		} else {
			Composite secondaryVideoParent = getSecondaryVideoParent();
			if (secondaryVideoParent != null) {
				return secondaryVideoParent;
				
			} else {
				return localMediaFrameParent;
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Full screen stuff.
	
	private FullscreenShell fullscreenShell = null;
	
	private boolean isFullScreen () {
		return !(fullscreenShell == null);
	}
	
	private Composite getFullScreenVideoParent () {
		if (!isFullScreen()) return null;
		return fullscreenShell.getShell();
	}
	
	private void goFullScreenSafe () {
		goFullScreenSafe(null, null);
	}
	
	private void goFullScreenSafe (Monitor mon, FullScreenAction action) {
		GoFullScreenRunner runner = new GoFullScreenRunner(mon, action);
		if (Thread.currentThread().equals(getSite().getShell().getDisplay().getThread())) {
			runner.run();
		} else {
			getSite().getShell().getDisplay().asyncExec(runner);
		}
	}
	
	private void removeFullScreenSafe (boolean closeShell) {
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
			if (mon==null || action == null) {
				Monitor currentMon = null;
				for (Monitor mon : getSite().getShell().getDisplay().getMonitors()) {
					if (mon.getBounds().contains(getSite().getShell().getDisplay().getCursorLocation())) {
						currentMon = mon;
						break;
					}
				}
				if (currentMon!=null) {
					for (FullScreenAction a : fullScreenActions) {
						if (a.getMonitor().equals(currentMon)) {
							goFullscreen(currentMon, a);
						}
					}
				}
				
			} else {
				goFullscreen(mon, action);
			}
		}
		
		private void goFullscreen (Monitor mon, Action action) {
			try {
				if (isFullScreen()) {
					new RemoveFullScreenRunner(true).run();
					action.setChecked(false);
					
				} else {
					startFullScreen(mon, action);
				}
				
			} catch (MorriganException e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
		}
		
		private void startFullScreen (Monitor mon, final Action action) throws ImplException {
			fullscreenShell = new FullscreenShell(getSite().getShell(), mon, new Runnable() {
				@Override
				public void run() {
					removeFullScreenSafe(false);
					action.setChecked(false);
				}
			});
			
			registerScreenPainter(fullscreenShell.getScreenPainter());
			fullscreenShell.getShell().open();
			updateCurrentMediaFrameParent();
			action.setChecked(true);
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
				unregisterScreenPainter(fullscreenShell.getScreenPainter());
				
				if (closeShell) fullscreenShell.getShell().close();
				
				FullscreenShell fs = fullscreenShell;
				fullscreenShell = null;
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
	private List<FullScreenAction> fullScreenActions = new ArrayList<FullScreenAction>();
	
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
			orderMenuActions.add(a);
		}
		
		// Full screen menu.
		for (int i = 0; i < parent.getShell().getDisplay().getMonitors().length; i++) {
			Monitor mon = parent.getShell().getDisplay().getMonitors()[i];
			FullScreenAction a = new FullScreenAction(i, mon);
			fullScreenActions.add(a);
		}
	}
	
	protected List<OrderSelectAction> getOrderMenuActions () {
		return orderMenuActions;
	}
	
	protected List<FullScreenAction> getFullScreenActions () {
		return fullScreenActions;
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
				getPlayer().setPlaybackOrder(mode);
				orderChangedListener.orderChanged(mode);
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
			return mon;
		}
		
		@Override
		public void run() {
			goFullScreenSafe(mon, this);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	protected IAction pauseAction = new Action("Pause", Activator.getImageDescriptor("icons/pause.gif")) {
		@Override
		public void run() {
			getPlayer().pausePlaying();
		};
	};
	
	protected IAction stopAction = new Action("Stop", Activator.getImageDescriptor("icons/stop.gif")) {
		@Override
		public void run() {
			getPlayer().stopPlaying();
		};
	};
	
	protected IAction nextAction = new Action("Next", Activator.getImageDescriptor("icons/next.gif")) {
		@Override
		public void run() {
			getPlayer().nextTrack();
		};
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
		};
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
				
				try {
					if (getPlayer().getCurrentItem().list.getType().equals(LocalMediaLibrary.TYPE)) {
						LibraryEditorInput input = EditorFactory.getMediaLibraryInput(getPlayer().getCurrentItem().list.getListId());
						getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, LocalLibraryEditor.ID);
						
					} else if (getPlayer().getCurrentItem().list.getType().equals(MediaPlaylist.TYPE)) {
						MediaTrackListEditorInput<MediaPlaylist> input = EditorFactory.getMediaPlaylistInput(getPlayer().getCurrentItem().list.getListId());
						getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, PlaylistEditor.ID);
						
					}
					
					IEditorPart activeEditor = getViewSite().getWorkbenchWindow().getActivePage().getActiveEditor();
					if (activeEditor instanceof MediaTrackListEditor<?,?>) {
						MediaTrackListEditor<?,?> mediaListEditor = (MediaTrackListEditor<?,?>) activeEditor;
						mediaListEditor.revealTrack(getPlayer().getCurrentItem().item);
					}
					
				} catch (Exception e) {
					getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
				}
				
			}
		}
	};
	
	protected IAction jumpToAction = new Action ("Jump to...") {
		@Override
		public void run() {
			MediaItemList<? extends MediaItem> currentList = getPlayer().getCurrentList();
			if (currentList == null) return;
			if (!(currentList instanceof LocalMediaLibrary)) return;
			
			JumpToDlg dlg = new JumpToDlg(getViewSite().getShell(), (LocalMediaLibrary) currentList);
			PlayItem item = dlg.open();
			if (item != null) {
				if ((dlg.getKeyMask() & SWT.SHIFT) != 0 || (dlg.getKeyMask() & SWT.CONTROL) != 0) {
					getPlayer().addToQueue(item);
				} else {
					getPlayer().loadAndStartPlaying(item);
				}
			}
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Hotkeys.
	
	private void setupHotkeys () {
		try {
			HotkeyRegister.addHotkeyListener(hotkeyListener);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private void finaliseHotkeys () {
		try {
			HotkeyRegister.removeHotkeyListener(hotkeyListener);
			
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
							jumpToAction.run();
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

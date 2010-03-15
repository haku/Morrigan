package net.sparktank.morrigan.views;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.JumpToDlg;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.dialogs.RunnableDialog;
import net.sparktank.morrigan.display.FullscreenShell;
import net.sparktank.morrigan.display.ScreenPainter;
import net.sparktank.morrigan.display.ScreenPainter.TitleProvider;
import net.sparktank.morrigan.editors.EditorFactory;
import net.sparktank.morrigan.editors.LibraryEditor;
import net.sparktank.morrigan.editors.MediaListEditor;
import net.sparktank.morrigan.editors.MediaListEditorInput;
import net.sparktank.morrigan.editors.PlaylistEditor;
import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.HotkeyRegister;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ClipboardHelper;
import net.sparktank.morrigan.helpers.OrderHelper;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaPlaylist;
import net.sparktank.morrigan.model.media.PlayItem;

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
		finalisePlaybackEngine();
		finaliseHotkeys();
		setCurrentItem(null);
		
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
				setPlaybackOrder(OrderHelper.parsePlaybackOrderByName(modeName));
			}
		}
	}
	
	@Override
	public void saveState(IMemento memento) {
		memento.putString(KEY_ORDERMODE, getPlaybackOrder().name());
		
		super.saveState(memento);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.
	
	/**
	 * This will only ever be called on the UI thread.
	 */
	abstract protected void updateStatus ();
	
	abstract protected void orderModeChanged (PlaybackOrder order);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Current selection.
	
	private PlayItem _currentItem = null;
	
	/**
	 * This is called at the start of each track.
	 * Must call this with list a null before this
	 * object is disposed so as to remove listener.
	 */
	private void setCurrentItem (PlayItem item) {
		if (_currentItem != null && _currentItem.list != null) {
			_currentItem.list.removeChangeEvent(listChangedRunnable);
		}
		
		_currentItem = item;
		
		if (_currentItem != null && _currentItem.list != null) {
			_currentItem.list.addChangeEvent(listChangedRunnable);
			
			if (_currentItem.item != null) {
				addToHistory(_currentItem);
			}
		}
		
		updateTitle();
	}
	
	private Runnable listChangedRunnable = new Runnable() {
		@Override
		public void run() {
			validateHistory();
		}
	};
	
	protected PlayItem getCurrentItem () {
		// TODO check item is still valid.
		return _currentItem;
	}
	
	protected MediaList getCurrentList () {
		MediaList ret = null;
		
		PlayItem currentItem = getCurrentItem();
		if (currentItem != null && currentItem.list != null) {
			ret = currentItem.list;
			
		} else {
			IEditorPart activeEditor = getViewSite().getPage().getActiveEditor();
			if (activeEditor != null && activeEditor instanceof MediaListEditor<?>) {
				MediaListEditor<?> mediaListEditor = (MediaListEditor<?>) activeEditor;
				MediaList editedMediaList = mediaListEditor.getMediaList();
				ret = editedMediaList;
			}
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Track order methods.
	
	private PlaybackOrder _playbackOrder = PlaybackOrder.SEQUENTIAL;
	
	protected PlaybackOrder getPlaybackOrder () {
		return _playbackOrder;
	}
	
	protected void setPlaybackOrder (PlaybackOrder order) {
		_playbackOrder = order;
	}
	
	private PlayItem getNextItemToPlay () {
		PlayItem nextItem = null;
		
		if (isQueueHasItem()) {
			nextItem = readFromQueue();
			
		} else if (getCurrentItem() != null && getCurrentItem().list != null) {
			if (getCurrentItem().item != null) {
				MediaItem nextTrack = OrderHelper.getNextTrack(getCurrentItem().list, getCurrentItem().item, _playbackOrder);
				nextItem = new PlayItem(getCurrentItem().list, nextTrack);
			}
			
		} else {
			MediaList currentList = getCurrentList();
			if (currentList != null) {
				MediaItem nextTrack = OrderHelper.getNextTrack(currentList, null, _playbackOrder);
				nextItem = new PlayItem(currentList, nextTrack);
			}
		}
		
		return nextItem;
	}
	
	private class NextTrackRunner implements Runnable {
		
		private final PlayItem item;
		
		public NextTrackRunner (PlayItem item) {
			this.item = item;
		}
		
		@Override
		public void run() {
			loadAndStartPlaying(item);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	History.
	
	static private final int HISTORY_LENGTH = 10;
	
	private List<PlayItem> _history = new ArrayList<PlayItem>();
	private MenuManager _historyMenuMgr = new MenuManager();
	
	private void addToHistory (PlayItem item) {
		if (_history.contains(item)) {
			_history.remove(item);
		}
		_history.add(0, item);
		if (_history.size() > HISTORY_LENGTH) {
			_history.remove(_history.size()-1);
		}
		callRefreshHistoryMenuMgr();
	}
	
	private void validateHistory () {
		boolean changed = false;
		
		for (PlayItem item : _history) {
			if (!item.list.getMediaTracks().contains(item.item)) {
				_history.remove(item);
				changed = true;
			}
		}
		
		if (changed) {
			callRefreshHistoryMenuMgr();
		}
	}
	
	private class HistoryAction extends Action {
		
		private final PlayItem item;
		
		public HistoryAction (PlayItem item) {
			this.item = item;
			setText(item.item.getTitle());
		}
		
		@Override
		public void run() {
			loadAndStartPlaying(item.list, item.item);
		}
		
	}
	
	private volatile boolean refreshHistoryMenuMgrScheduled = false;
	
	private synchronized void callRefreshHistoryMenuMgr () {
		if (!refreshHistoryMenuMgrScheduled) {
			refreshHistoryMenuMgrScheduled = true;
			getSite().getShell().getDisplay().asyncExec(refreshHistoryMenuMgr);
		}
	}
	
	private Runnable refreshHistoryMenuMgr = new Runnable() {
		@Override
		public synchronized void run() {
			_historyMenuMgr.removeAll();
			for (PlayItem item : _history) {
				_historyMenuMgr.add(new HistoryAction(item));
			}
			refreshHistoryMenuMgrScheduled = false;
		}
	};
	
	protected MenuManager getHistoryMenuMgr () {
		return _historyMenuMgr;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue.
	
	private List<PlayItem> _queue = new ArrayList<PlayItem>();
	private List<Runnable> _queueChangeListeners = new ArrayList<Runnable>();
	
	public void addToQueue (PlayItem item) {
		_queue.add(item);
		callQueueChangedListeners();
	}
	
	public void removeFromQueue (PlayItem item) {
		_queue.remove(item);
		callQueueChangedListeners();
	}
	
	public void moveInQueue (List<PlayItem> items, boolean moveDown) {
		if (items == null || items.isEmpty()) return;
		
		for (	int i = (moveDown ? _queue.size() - 1 : 0);
				(moveDown ? i >= 0 : i < _queue.size());
				i = i + (moveDown ? -1 : 1)
			) {
			if (items.contains(_queue.get(i))) {
				int j;
				if (moveDown) {
					if (i == _queue.size() - 1 ) {
						j = -1;
					} else {
						j = i + 1;
					}
				} else {
					if (i == 0) {
						j = -1;
					} else {
						j = i - 1;
					}
				}
				if (j != -1 && !items.contains(_queue.get(j))) {
					PlayItem a = _queue.get(i);
					PlayItem b = _queue.get(j);
					_queue.set(i, b);
					_queue.set(j, a);
				}
			}
		}
		
		callQueueChangedListeners();
	}
	
	private boolean isQueueHasItem () {
		return !_queue.isEmpty();
	}
	
	private PlayItem readFromQueue () {
		if (!_queue.isEmpty()) {
			PlayItem item = _queue.remove(0);
			callQueueChangedListeners();
			return item;
		} else {
			return null;
		}
	}
	
	private void callQueueChangedListeners () {
		for (Runnable r : _queueChangeListeners) {
			r.run();
		}
	}
	
	public List<PlayItem> getQueueList () {
		return _queue;
	}
	
	public void addQueueChangeListener (Runnable listener) {
		_queueChangeListeners.add(listener);
	}
	
	public void removeQueueChangeListener (Runnable listener) {
		_queueChangeListeners.remove(listener);
	}
	
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
//	Playback engine.
	
	IPlaybackEngine playbackEngine = null;
	
	private IPlaybackEngine getPlaybackEngine () throws ImplException {
		return getPlaybackEngine(true);
	}
	
	private IPlaybackEngine getPlaybackEngine (boolean create) throws ImplException {
		if (playbackEngine == null && create) {
			playbackEngine = EngineFactory.makePlaybackEngine();
			playbackEngine.setStatusListener(playbackStatusListener);
		}
		
		return playbackEngine;
	}
	
	private void finalisePlaybackEngine () {
		IPlaybackEngine eng = null;
		
		try {
			eng = getPlaybackEngine(false);
		} catch (ImplException e) {
			e.printStackTrace();
		}
		
		if (eng!=null) {
			try {
				eng.stopPlaying();
			} catch (PlaybackException e) {
				e.printStackTrace();
			}
			eng.unloadFile();
			eng.finalise();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback management.
	
	private long _currentPosition = -1; // In seconds.
	private int _currentTrackDuration = -1; // In seconds.
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (MediaList list, MediaItem track) {
		loadAndStartPlaying(new PlayItem(list, track));
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (PlayItem item) {
		if (getSite().getShell().getDisplay().getThread().getId() != Thread.currentThread().getId()) {
			System.out.println("Starting playback not on UI thread!");
		}
		
		try {
			setCurrentItem(item);
			
			File file = new File(item.item.getFilepath());
			if (!file.exists()) throw new FileNotFoundException(item.item.getFilepath());
			
			getPlaybackEngine().setFile(item.item.getFilepath());
			getPlaybackEngine().setVideoFrameParent(getCurrentMediaFrameParent());
			getPlaybackEngine().startPlaying();
			_currentTrackDuration = getPlaybackEngine().getDuration();
			System.out.println("Started to play " + item.item.getTitle());
			
			item.list.incTrackStartCnt(item.item);
			if (item.item.getDuration() <= 0) {
				if (_currentTrackDuration > 0) {
					item.list.setTrackDuration(item.item, _currentTrackDuration);
				}
			}
			
		} catch (Exception e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
		
		callUpdateStatus();
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void pausePlaying () {
		try {
			internal_pausePlaying();
		} catch (PlaybackException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void stopPlaying () {
		try {
			internal_stopPlaying();
		} catch (PlaybackException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	public void nextTrack () {
		PlayItem nextItemToPlay = getNextItemToPlay();
		if (nextItemToPlay != null) {
			loadAndStartPlaying(nextItemToPlay);
		}
	}
	
	public void seekTo (double d) {
		try {
			internal_seekTo(d);
		} catch (PlaybackException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	private void internal_pausePlaying () throws PlaybackException {
		// Don't go and make a player engine instance.
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			PlayState playbackState = eng.getPlaybackState();
			
			if (playbackState == PlayState.Paused) {
				eng.resumePlaying();
				
			} else if (playbackState == PlayState.Playing) {
				eng.pausePlaying();
				
			} else if (playbackState == PlayState.Stopped) {
				loadAndStartPlaying(getCurrentItem());
				
			} else {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog("Don't know what to do.  Playstate=" + playbackState + "."));
			}
			callUpdateStatus();
		}
	}
	
	/**
	 * For internal use.  Does not update GUI.
	 * @throws ImplException
	 * @throws PlaybackException
	 */
	private void internal_stopPlaying () throws ImplException, PlaybackException {
		/* Don't go and make a player engine instance
		 * just to call stop on it.
		 */
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			eng.stopPlaying();
			eng.unloadFile();
			
			callUpdateStatus();
		}
	}
	
	protected void internal_seekTo (double d) throws PlaybackException {
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			eng.seekTo(d);
		}
	}
	
	private Runnable updateStatusRunable = new Runnable() {
		@Override
		public void run() {
			updateStatus();
		}
	};
	
	/**
	 * This way, it can only ever be called by the right thread.
	 */
	protected void callUpdateStatus () {
		getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
	}
	
	private IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {
		
		@Override
		public void positionChanged(long position) {
			_currentPosition = position;
			callUpdateStatus();
		}
		
		@Override
		public void durationChanged(int duration) {
			_currentTrackDuration = duration;
			callUpdateStatus();
		};
		
		@Override
		public void statusChanged(PlayState state) {
			
		}
		
		@Override
		public void onEndOfTrack() {
			// Inc. stats.
			try {
				getCurrentItem().list.incTrackEndCnt(getCurrentItem().item);
			} catch (MorriganException e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
			
			// Play next track?
			PlayItem nextItemToPlay = getNextItemToPlay();
			if (nextItemToPlay != null) {
				getSite().getShell().getDisplay().asyncExec(new NextTrackRunner(nextItemToPlay));
				
			} else {
				System.out.println("No more tracks to play.");
				callUpdateStatus();
			}
		};
		
		@Override
		public void onError(Exception e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
		
		@Override
		public void onKeyPress(int keyCode) {
//			System.out.println("key released: " + keyCode);
			if (keyCode == SWT.ESC) {
				if (isFullScreen()) {
					removeFullScreenSafe(true);
				}
			}
		}
		
		@Override
		public void onMouseClick(int button, int clickCount) {
			System.out.println("Mouse click "+button+"*"+clickCount);
			if (clickCount > 1) {
				if (!isFullScreen()) {
					goFullScreenSafe();
				} else {
					removeFullScreenSafe(true);
				}
			}
		}
		
	};
	
	protected PlayState getPlayState () {
		try {
			IPlaybackEngine eng = getPlaybackEngine(false);
			if (eng!=null) {
				return eng.getPlaybackState();
			} else {
				return PlayState.Stopped;
			}
		} catch (ImplException e) {
			return PlayState.Stopped;
		}
	}
	
	protected long getCurrentPosition () {
		return _currentPosition;
	}
	
	protected int getCurrentTrackDuration () {
		return _currentTrackDuration;
	}
	
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
			return getCurrentItem();
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
		Composite cmfp = getCurrentMediaFrameParent();
		videoParentChanged(cmfp);
		
		IPlaybackEngine engine;
		try {
			engine = getPlaybackEngine(false);
		} catch (ImplException e) {
			throw new RuntimeException(e);
		}
		if (engine!=null) {
			engine.setVideoFrameParent(cmfp);
		}
	}
	
	protected Composite getCurrentMediaFrameParent () {
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
			if (getPlaybackOrder() == o) a.setChecked(true);
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
				setPlaybackOrder(mode);
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
			pausePlaying();
		};
	};
	
	protected IAction stopAction = new Action("Stop", Activator.getImageDescriptor("icons/stop.gif")) {
		@Override
		public void run() {
			stopPlaying();
		};
	};
	
	protected IAction nextAction = new Action("Next", Activator.getImageDescriptor("icons/next.gif")) {
		@Override
		public void run() {
			nextTrack();
		};
	};
	
	protected IAction copyPathAction = new Action("Copy file path") {
		@Override
		public void run() {
			PlayItem currentItem = getCurrentItem();
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
			if (getCurrentItem() != null
					&& getCurrentItem().list != null
					&& getCurrentItem().item != null) {
				
				try {
					if (getCurrentItem().list.getType() == MediaLibrary.TYPE) {
						MediaListEditorInput<MediaLibrary> input = EditorFactory.getMediaLibraryInput(getCurrentItem().list.getListId());
						getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, LibraryEditor.ID);
						
					} else if (getCurrentItem().list.getType() == MediaPlaylist.TYPE) {
						MediaListEditorInput<MediaPlaylist> input = EditorFactory.getMediaPlaylistInput(getCurrentItem().list.getListId());
						getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, PlaylistEditor.ID);
						
					}
					
					IEditorPart activeEditor = getViewSite().getWorkbenchWindow().getActivePage().getActiveEditor();
					if (activeEditor instanceof MediaListEditor<?>) {
						MediaListEditor<?> mediaListEditor = (MediaListEditor<?>) activeEditor;
						mediaListEditor.revealTrack(getCurrentItem().item);
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
			MediaList currentList = getCurrentList();
			if (currentList == null) return;
			if (!(currentList instanceof MediaLibrary)) return;
			
			JumpToDlg dlg = new JumpToDlg(getViewSite().getShell(), (MediaLibrary) currentList);
			PlayItem item = dlg.open();
			if (item != null) {
				if ((dlg.getKeyMask() & SWT.SHIFT) != 0 || (dlg.getKeyMask() & SWT.CONTROL) != 0) {
					addToQueue(item);
				} else {
					loadAndStartPlaying(item);
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
			IPlaybackEngine eng = null;
			try {
				eng = getPlaybackEngine(false);
			} catch (ImplException e) {
				e.printStackTrace();
			}
			
			boolean isPlaying = false;
			boolean isPaused = false;
			if (eng != null) {
				PlayState playbackState = eng.getPlaybackState();
				if (playbackState == PlayState.Playing) {
					isPlaying = true;
				}else if (playbackState == PlayState.Paused) {
					isPaused = true;
				}
			}
			
			switch (id) {
				
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
					} else if (isPaused || getCurrentList() != null) {
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
							try {
								internal_stopPlaying();
							} catch (Exception e) {
								e.printStackTrace();
							}
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
							try {
								internal_pausePlaying();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					break;
					
				case IHotkeyEngine.MORRIGAN_HK_NEXT:
					getSite().getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								nextTrack();
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

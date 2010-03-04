package net.sparktank.morrigan.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.dialogs.RunnableDialog;
import net.sparktank.morrigan.display.FullscreenShell;
import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.HotkeyRegister;
import net.sparktank.morrigan.engines.common.ImplException;
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
import net.sparktank.morrigan.model.media.MediaList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.part.ViewPart;

/**
 * TODO tidy this class.
 */
public abstract class AbstractPlayerView extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaList currentList = null;
	private MediaItem currentItem = null;
	private long currentPosition = -1; // In seconds.
	private PlaybackOrder playbackOrder = PlaybackOrder.SEQUENTIAL;
	
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
		
		super.dispose();
	}
	
	protected boolean isDisposed () {
		return isDisposed;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.
	
	/**
	 * This will only ever be called on the UI thread.
	 */
	abstract protected void updateStatus ();
	
	abstract protected void orderModeChanged (PlaybackOrder order);
	
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
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (MediaList list, MediaItem track) {
		if (getSite().getShell().getDisplay().getThread().getId() != Thread.currentThread().getId()) {
			System.out.println("Starting playback not on UI thread!");
		}
		
		try {
			currentList = list;
			currentItem = track;
			getPlaybackEngine().setFile(currentItem.getFilepath());
			getPlaybackEngine().setVideoFrameParent(getCurrentMediaFrameParent());
			getPlaybackEngine().startPlaying();
			System.out.println("Started to play " + currentItem.getTitle());
			
			currentList.incTrackStartCnt(currentItem);
			
		} catch (MorriganException e) {
			currentItem = null;
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
			callUpdateStatus();
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
		}
		
		currentItem = null;
	}
	
	private MediaItem getNextTrackToPlay () {
		if (currentList==null || currentItem==null) return null;
		return OrderHelper.getNextTrack(currentList, currentItem, playbackOrder);
	}
	
	protected PlaybackOrder getPlaybackOrder () {
		return playbackOrder;
	}
	
	protected MediaList getCurrentList () {
		return currentList;
	}
	
	protected MediaItem getCurrentItem () {
		return currentItem;
	}
	
	protected long getCurrentPosition () {
		return currentPosition;
	}
	
	private IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {
		
		@Override
		public void positionChanged(long position) {
			currentPosition = position;
			callUpdateStatus();
		}
		
		@Override
		public void statusChanged(PlayState state) {
			
		}
		
		@Override
		public void onEndOfTrack() {
			// Inc. stats.
			try {
				currentList.incTrackEndCnt(currentItem);
			} catch (MorriganException e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
			
			// Play next track?
			MediaItem nextTrackToPlay = getNextTrackToPlay();
			if (nextTrackToPlay != null) {
				getSite().getShell().getDisplay().asyncExec(new NextTrackRunner(currentList, nextTrackToPlay));
				
			} else {
				System.out.println("No more tracks to play.");
				
				currentItem = null;
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
	
	private class NextTrackRunner implements Runnable {
		
		private final MediaList list;
		private final MediaItem track;
		
		public NextTrackRunner (MediaList list, MediaItem track) {
			this.list = list;
			this.track = track;
		}
		
		@Override
		public void run() {
			loadAndStartPlaying(list, track);
		}
		
	}
	
	protected PlayState internal_getPlayState () {
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Video frame parent stuff.
	
	private Composite currentMediaFrameParent;
	private Composite localMediaFrameParent;
	
	protected void setLocalMediaFrameParent (Composite parent) {
		localMediaFrameParent = parent;
	}
	
	protected Composite getLocalMediaFrameParent () {
		if (localMediaFrameParent==null) throw new IllegalAccessError("setMediaFrameParent() has not yet been called.");
		return localMediaFrameParent;
	}
	
	protected void setCurrentMediaFrameParent (Composite frame) throws ImplException {
		currentMediaFrameParent = frame;
		
		IPlaybackEngine engine = getPlaybackEngine(false);
		if (engine!=null) {
			engine.setVideoFrameParent(getCurrentMediaFrameParent());
		}
	}
	
	private Composite getCurrentMediaFrameParent () {
		if (currentMediaFrameParent == null) {
			return localMediaFrameParent;
		} else {
			return currentMediaFrameParent;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Full screen stuff.
	
	private FullscreenShell fullscreenShell = null;
	
	private boolean isFullScreen () {
		return !(fullscreenShell==null);
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
			
			setCurrentMediaFrameParent(fullscreenShell.getShell());
			
			action.setChecked(true);
			fullscreenShell.getShell().open();
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
				if (closeShell) fullscreenShell.getShell().close();
				
				setCurrentMediaFrameParent(null);
				
				if (fullscreenShell!=null) {
					if (!fullscreenShell.getShell().isDisposed()) fullscreenShell.getShell().dispose();
					fullscreenShell = null;
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
			if (playbackOrder == o) a.setChecked(true);
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
				playbackOrder = mode;
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
		public void run() {
			pausePlaying();
		};
	};
	
	protected IAction stopAction = new Action("Stop", Activator.getImageDescriptor("icons/stop.gif")) {
		public void run() {
			stopPlaying();
		};
	};
	
	protected IAction nextAction = new Action("Next", Activator.getImageDescriptor("icons/next.gif")) {
		public void run() {
			MediaItem nextTrackToPlay = getNextTrackToPlay();
			if (nextTrackToPlay != null) {
				loadAndStartPlaying(currentList, nextTrackToPlay);
			}
		};
	};
	
	protected IAction prevAction = new Action("Previous", Activator.getImageDescriptor("icons/prev.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement previous desu~.").open();
		};
	};
	
	protected IAction copyPathAction = new Action("Copy file path") {
		public void run() {
			if (currentItem!=null) {
				ClipboardHelper.setText(currentItem.getFilepath(), Display.getCurrent());
			
			} else {
				new MorriganMsgDlg("No track loaded desu~.").open();
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
		public void onKeyPress(int id) {
			switch (id) {
				
				case HotkeyRegister.HK_PLAYPAUSE:
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
							} catch (PlaybackException e) {
								e.printStackTrace();
							}
						}
					});
					break;
				
				default:
					getSite().getShell().getDisplay().asyncExec(new RunnableDialog("id="+id));
					break;
				
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

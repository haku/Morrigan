package net.sparktank.morrigan.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.dialogs.RunnableDialog;
import net.sparktank.morrigan.display.FullscreenShell;
import net.sparktank.morrigan.display.ScreenPainter;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ClipboardHelper;
import net.sparktank.morrigan.helpers.OrderHelper;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.ImplException;
import net.sparktank.morrigan.playback.PlaybackEngineFactory;
import net.sparktank.morrigan.playback.PlaybackException;
import net.sparktank.morrigan.playback.IPlaybackEngine.PlayState;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.part.ViewPart;

public class ViewPlayer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewPlayer";
	
	private MediaList currentList = null;
	private MediaItem currentTrack = null;
	private long currentPosition = -1; // In seconds.
	private PlaybackOrder playbackOrder = PlaybackOrder.SEQUENTIAL;
	
	private volatile boolean isDisposed = false;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	public void createPartControl(Composite parent) {
		makeIcons();
		makeControls(parent);
		addToolbar();
		addMenu();
		getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
	}
	
	@Override
	public void setFocus() {
		mediaFrameParent.setFocus();
	}
	
	@Override
	public void dispose() {
		isDisposed = true;
		finalisePlaybackEngine();
		disposeIcons();
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback engine.
	
	IPlaybackEngine playbackEngine = null;
	
	private IPlaybackEngine getPlaybackEngine () throws ImplException {
		return getPlaybackEngine(true);
	}
	
	private IPlaybackEngine getPlaybackEngine (boolean create) throws ImplException {
		if (playbackEngine == null && create) {
			playbackEngine = PlaybackEngineFactory.makePlaybackEngine();
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
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (MediaList list, MediaItem track) {
		try {
			currentList = list;
			currentTrack = track;
			getPlaybackEngine().setFile(currentTrack.getFilepath());
			getPlaybackEngine().setVideoFrameParent(getCurrentMediaFrameParent());
			getPlaybackEngine().startPlaying();
			System.out.println("Started to play " + currentTrack.getTitle());
			
			currentList.incTrackStartCnt(currentTrack);
			
		} catch (MorriganException e) {
			currentTrack = null;
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
		
		getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void pausePlaying () {
		try {
			internal_pausePlaying();
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
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
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
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
				new MorriganMsgDlg("Don't know what to do.  Playstate=" + playbackState + ".").open();
			}
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
		
		currentTrack = null;
	}
	
	private MediaItem getNextTrackToPlay () {
		if (currentList==null || currentTrack==null) return null;
		return OrderHelper.getNextTrack(currentList, currentTrack, playbackOrder);
	}
	
	private IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {
		
		@Override
		public void positionChanged(long position) {
			currentPosition = position;
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
		}
		
		@Override
		public void statusChanged(PlayState state) {
			
		}
		
		@Override
		public void onEndOfTrack() {
			// Inc. stats.
			try {
				currentList.incTrackEndCnt(currentTrack);
			} catch (MorriganException e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
			
			// Play next track?
			MediaItem nextTrackToPlay = getNextTrackToPlay();
			if (nextTrackToPlay != null) {
				getSite().getShell().getDisplay().asyncExec(new NextTrackRunner(currentList, nextTrackToPlay));
				
			} else {
				System.out.println("No more tracks to play.");
				
				currentTrack = null;
				getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
			}
		};
		
		@Override
		public void onError(Exception e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
		
		@Override
		public void onKeyPress(int keyCode) {
			System.out.println("key released: " + keyCode);
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
	
	private PlayState internal_getPlayState () {
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
//	Full screen stuff.
	
	private FullscreenShell fullscreenShell = null;
	
	private Composite getCurrentMediaFrameParent () {
		if (fullscreenShell!=null) {
			return fullscreenShell.getShell();
		} else {
			return mediaFrameParent;
		}
	}
	
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
		private final FullScreenAction action;

//		public GoFullScreenRunner () {
//			this.mon = null;
//			this.action = null;
//		}
		
		public GoFullScreenRunner (Monitor mon, FullScreenAction action) {
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
		
		private void goFullscreen (Monitor mon, FullScreenAction action) {
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
		
		private void startFullScreen (Monitor mon, final FullScreenAction action) throws ImplException {
			fullscreenShell = new FullscreenShell(getSite().getShell(), mon, new Runnable() {
				@Override
				public void run() {
					removeFullScreenSafe(false);
					action.setChecked(false);
				}
			});
			
			IPlaybackEngine engine = getPlaybackEngine(false);
			if (engine!=null) {
				engine.setVideoFrameParent(fullscreenShell.getShell());
			}
			
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
				
				IPlaybackEngine engine = getPlaybackEngine(false);
				if (engine!=null) {
					engine.setVideoFrameParent(mediaFrameParent);
				}
				
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
//	GUI stuff.
	
	private Image iconPlay;
	private Image iconPause;
	private Image iconStop;
	
	private void makeIcons () {
		iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		iconPause = Activator.getImageDescriptor("icons/pause.gif").createImage();
		iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
	}
	
	private void disposeIcons () {
		iconPlay.dispose();
		iconPause.dispose();
		iconStop.dispose();
	}
	
	private Canvas mediaFrameParent;
	private List<OrderSelectAction> orderMenuActions = new ArrayList<OrderSelectAction>();
	private List<FullScreenAction> fullScreenActions = new ArrayList<FullScreenAction>();
	
	private void makeControls (Composite parent) {
		// Main label.
		parent.setLayout(new FillLayout());
		parent.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				mediaFrameParent.setFocus();
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});
		
		mediaFrameParent = new Canvas(parent, SWT.NONE);
		mediaFrameParent.setLayout(new FillLayout());
		mediaFrameParent.addPaintListener(new ScreenPainter(mediaFrameParent));
		
		// Order menu.
		for (PlaybackOrder o : PlaybackOrder.values()) {
			OrderSelectAction a = new OrderSelectAction(o);
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
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(pauseAction);
		getViewSite().getActionBars().getToolBarManager().add(stopAction);
		getViewSite().getActionBars().getToolBarManager().add(nextAction);
		getViewSite().getActionBars().getToolBarManager().add(new Separator());
	}
	
	private void addMenu () {
		for (OrderSelectAction a : orderMenuActions) {
			getViewSite().getActionBars().getMenuManager().add(a);
		}
		
		getViewSite().getActionBars().getMenuManager().add(new Separator());
		
		getViewSite().getActionBars().getMenuManager().add(pauseAction);
		getViewSite().getActionBars().getMenuManager().add(stopAction);
		getViewSite().getActionBars().getMenuManager().add(prevAction);
		getViewSite().getActionBars().getMenuManager().add(nextAction);
		
		getViewSite().getActionBars().getMenuManager().add(new Separator());
		
		for (FullScreenAction a : fullScreenActions) {
			getViewSite().getActionBars().getMenuManager().add(a);
		}
		
		getViewSite().getActionBars().getMenuManager().add(new Separator());
		
		getViewSite().getActionBars().getMenuManager().add(copyPathAction);
	}
	
	/**
	 * This way, it can only ever be called by the right thread.
	 */
	private Runnable updateStatusRunable = new Runnable() {
		public void run() {
			if (isDisposed) return;
			
			if (currentTrack != null && currentList != null) {
				
				switch (internal_getPlayState()) {
					case Playing:
						setTitleImage(iconPlay);
						break;
					
					case Paused:
						setTitleImage(iconPause);
						break;
					
					case Loading:
						setTitleImage(iconPlay); // FIXME new icon?
						break;
					
					case Stopped:
						setTitleImage(iconStop);
						break;
					
				}
				
				setContentDescription(
//						"Now playing: " + currentTrack.toString() +
//						"\n   From: " + currentList.getListName() +
//						"\n   Position: " + currentPosition
						
						"Playing: " + currentPosition + " : " + currentTrack.toString()
						);
				
			} else {
				setTitleImage(iconStop);
				setContentDescription("Idle.");
			};
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Complex actions.
	
	private class OrderSelectAction extends Action {
		
		private final PlaybackOrder mode;
		
		public OrderSelectAction (PlaybackOrder mode) {
			super(mode.toString(), AS_RADIO_BUTTON);
			this.mode = mode;
		}
		
		@Override
		public void run() {
			if (isChecked()) {
				playbackOrder = mode;
			}
		}
		
	}
	
	private class FullScreenAction extends Action {
		
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
	
	private IAction pauseAction = new Action("Pause", Activator.getImageDescriptor("icons/pause.gif")) {
		public void run() {
			pausePlaying();
		};
	};
	
	private IAction stopAction = new Action("Stop", Activator.getImageDescriptor("icons/stop.gif")) {
		public void run() {
			stopPlaying();
		};
	};
	
	private IAction nextAction = new Action("Next", Activator.getImageDescriptor("icons/next.gif")) {
		public void run() {
			MediaItem nextTrackToPlay = getNextTrackToPlay();
			if (nextTrackToPlay != null) {
				loadAndStartPlaying(currentList, nextTrackToPlay);
			}
		};
	};
	
	private IAction prevAction = new Action("Previous", Activator.getImageDescriptor("icons/prev.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement previous desu~.").open();
		};
	};
	
	private IAction copyPathAction = new Action("Copy file path") {
		public void run() {
			if (currentTrack!=null) {
				ClipboardHelper.setText(currentTrack.getFilepath(), Display.getCurrent());
			
			} else {
				new MorriganMsgDlg("No track loaded desu~.").open();
			}
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

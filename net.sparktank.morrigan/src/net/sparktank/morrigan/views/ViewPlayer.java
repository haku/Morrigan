package net.sparktank.morrigan.views;

import java.awt.Color;
import java.awt.Frame;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.dialogs.RunnableDialog;
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
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
	public void setFocus() {}
	
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
			internal_stopPlaying();
			currentList = list;
			currentTrack = track;
			getPlaybackEngine().setFile(currentTrack.getFilepath());
			getPlaybackEngine().setVideoFrame(mediaFrame);
			getPlaybackEngine().startPlaying();
			
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
		return OrderHelper.getNextTrack(currentList, currentTrack, orderSelecter.getSelectedOrder());
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
				loadAndStartPlaying(currentList, nextTrackToPlay);
				
			} else {
				currentTrack = null;
				getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
			}
		};
		
		@Override
		public void onError(Exception e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
		
	};
	
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
	
//	private Label mainLabel;
	private Frame mediaFrame;
	private OrderSelecter orderSelecter;
	
	private void makeControls (Composite parent) {
		// Main label.
		parent.setLayout(new FillLayout ());
//		mainLabel = new Label(parent, SWT.WRAP);
		
		Composite composite = new Composite(parent, SWT.EMBEDDED);
        composite.setLayout(new FillLayout( ));
		
        mediaFrame = SWT_AWT.new_Frame(composite);
        mediaFrame.setBackground(Color.BLACK);
        
		// Order drop-down box.
		orderSelecter = new OrderSelecter("orderSelecter");
	}
	
	class OrderSelecter extends ControlContribution {
		
		private Combo c;
		String selected = playbackOrder.toString();
		
		protected OrderSelecter(String id) {
			super(id);
		}
		
		@Override
		protected Control createControl(Composite parent) {
			c = new Combo(parent, SWT.READ_ONLY);
			for (PlaybackOrder o : PlaybackOrder.values()) {
				c.add(o.toString());
			}
			
			c.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					selected = c.getText();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					selected = c.getText();
				}
			});
			
			c.setText(playbackOrder.toString());
			
			return c;
		}
		
		public PlaybackOrder getSelectedOrder () {
			return OrderHelper.parsePlaybackOrder(selected);
		}
		
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(pauseAction);
		getViewSite().getActionBars().getToolBarManager().add(stopAction);
		getViewSite().getActionBars().getToolBarManager().add(prevAction);
		getViewSite().getActionBars().getToolBarManager().add(nextAction);
		
		getViewSite().getActionBars().getToolBarManager().add(new Separator());
		getViewSite().getActionBars().getToolBarManager().add(orderSelecter);
	}
	
	private void addMenu () {
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
//	Actions.
	
	private IAction pauseAction = new Action("pause", Activator.getImageDescriptor("icons/pause.gif")) {
		public void run() {
			pausePlaying();
		};
	};
	
	private IAction stopAction = new Action("stop", Activator.getImageDescriptor("icons/stop.gif")) {
		public void run() {
			stopPlaying();
		};
	};
	
	private IAction nextAction = new Action("next", Activator.getImageDescriptor("icons/next.gif")) {
		public void run() {
			MediaItem nextTrackToPlay = getNextTrackToPlay();
			if (nextTrackToPlay != null) {
				loadAndStartPlaying(currentList, nextTrackToPlay);
			}
		};
	};
	
	private IAction prevAction = new Action("previous", Activator.getImageDescriptor("icons/prev.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement previous desu~.").open();
		};
	};
	
	private IAction copyPathAction = new Action("copy file path") {
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

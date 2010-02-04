package net.sparktank.morrigan.views;

import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.helpers.ClipboardHelper;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaTrack;
import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.ImplException;
import net.sparktank.morrigan.playback.PlaybackEngineFactory;
import net.sparktank.morrigan.playback.PlaybackException;
import net.sparktank.morrigan.playback.IPlaybackEngine.PlayState;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class ViewPlayer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewPlayer";
	
	private Label mainLabel;
	
	private MediaList currentList = null;
	private MediaTrack currentTrack = null;
	
	private long currentPosition = -1;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout ());
		mainLabel = new Label(parent, SWT.WRAP);
		
		makeIcons();
		addToolbar();
		addMenu();
		getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
	}
	
	@Override
	public void setFocus() {}
	
	@Override
	public void dispose() {
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
			playbackEngine.setOnfinishHandler(atEndOfTrack);
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
	public void loadAndStartPlaying (MediaList list, MediaTrack track) {
		try {
			internal_stopPlaying();
			currentList = list;
			currentTrack = track;
			getPlaybackEngine().setFile(currentTrack.getFilepath());
			getPlaybackEngine().startPlaying();
			
		} catch (PlaybackException e) {
			currentTrack = null;
			e.printStackTrace();
			new MorriganMsgDlg(e).open();
		}
		
		getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void stopPlaying () {
		try {
			/* Don't go and make a player engine instance
			 * just to call stop on it.
			 */
			internal_stopPlaying();
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
		} catch (PlaybackException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	/**
	 * For internal use.  Does not update GUI.
	 * @throws ImplException
	 * @throws PlaybackException
	 */
	private void internal_stopPlaying () throws ImplException, PlaybackException {
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			eng.stopPlaying();
			eng.unloadFile();
		}
		
		currentTrack = null;
		
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
		
	};
	
	private Runnable atEndOfTrack = new Runnable() {
		@Override
		public void run() {
			List<MediaTrack> mediaTracks = currentList.getMediaTracks();
			if (mediaTracks.contains(currentTrack)) {
				int i = mediaTracks.indexOf(currentTrack) + 1;
				if (i >= mediaTracks.size()) i = 0;
				
				loadAndStartPlaying(currentList, mediaTracks.get(i));
				
			} else {
				currentTrack = null;
			}
			
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private Image iconPlay;
	private Image iconStop;
	
	private void makeIcons () {
		iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
	}
	
	private void disposeIcons () {
		iconPlay.dispose();
		iconStop.dispose();
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(pauseAction);
		getViewSite().getActionBars().getToolBarManager().add(stopAction);
		getViewSite().getActionBars().getToolBarManager().add(prevAction);
		getViewSite().getActionBars().getToolBarManager().add(nextAction);
	}
	
	private void addMenu () {
		getViewSite().getActionBars().getMenuManager().add(copyPathAction);
	}
	
	/**
	 * This way, it can only ever be called by the right thread.
	 */
	private Runnable updateStatusRunable = new Runnable() {
		public void run() {
			if (getSite().getShell().getDisplay().isDisposed()) return;
			
			if (currentTrack != null) {
				setTitleImage(iconPlay);
				mainLabel.setText(
						"Now playing: " + currentTrack.toString() +
						"\n   Position: " + currentPosition
						);
				
			} else {
				setTitleImage(iconStop);
				mainLabel.setText("Idle.");
			};
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private IAction pauseAction = new Action("pause", Activator.getImageDescriptor("icons/pause.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement pause desu~.").open();
		};
	};
	
	private IAction stopAction = new Action("stop", Activator.getImageDescriptor("icons/stop.gif")) {
		public void run() {
			stopPlaying();
		};
	};
	
	private IAction nextAction = new Action("next", Activator.getImageDescriptor("icons/next.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement next desu~.").open();
		};
	};
	
	private IAction prevAction = new Action("previous", Activator.getImageDescriptor("icons/prev.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement prev desu~.").open();
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

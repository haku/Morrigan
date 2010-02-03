package net.sparktank.morrigan.views;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.model.media.MediaTrack;
import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.ImplException;
import net.sparktank.morrigan.playback.PlaybackException;
import net.sparktank.morrigan.var.Const;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class ViewPlayer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewPlayer";
	
	private Label mainLabel;
	
	private MediaTrack currentTrack = null; 

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout ());
		mainLabel = new Label(parent, SWT.WRAP);
		
		addToolbar();
		addMenu();
		
		updateStatus();
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
	
	@Override
	public void setFocus() {}
	
	@Override
	public void dispose() {
		finalisePlaybackEngine();
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
			Class<?> [] classParm = null;
			Object [] objectParm = null;
			
			try {
				Class<?> cl = Class.forName(Const.PLAYBACK_ENGINE);
				java.lang.reflect.Constructor<?> co = cl.getConstructor(classParm);
				playbackEngine = (IPlaybackEngine) co.newInstance(objectParm);
				
			} catch (Exception e) {
				throw new ImplException(e);
			}
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
	public void loadAndStartPlaying (MediaTrack track) {
		try {
			getPlaybackEngine().stopPlaying();
			getPlaybackEngine().unloadFile();
			
			currentTrack = track;
			
			getPlaybackEngine().setFile(currentTrack.getFilepath());
			getPlaybackEngine().setOnfinishHandler(atEndOfTrack);
			
			getPlaybackEngine().startPlaying();
			
		} catch (PlaybackException e) {
			currentTrack = null;
			e.printStackTrace();
			new MorriganMsgDlg(e).open();
		}
		
		updateStatus();
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void startPlaying () {
		try {
			getPlaybackEngine().startPlaying();
			updateStatus();
		} catch (PlaybackException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void stopPlaying () {
		try {
			/* Don't go and make a player engine instnace
			 * just to call stop on it.
			 */
			IPlaybackEngine eng = getPlaybackEngine(false);
			if (eng!=null) eng.stopPlaying();
			updateStatus();
		} catch (PlaybackException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	private Runnable atEndOfTrack = new Runnable() {
		@Override
		public void run() {
			currentTrack = null;
			getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
		}
	};
	
	private Runnable updateStatusRunable = new Runnable() {
		public void run() {
			updateStatus();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private IAction pauseAction = new Action("pause", Activator.getImageDescriptor("icons/pause.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement pause.").open();
		};
	};
	
	private IAction stopAction = new Action("stop", Activator.getImageDescriptor("icons/stop.gif")) {
		public void run() {
			stopPlaying();
		};
	};
	
	private IAction nextAction = new Action("next", Activator.getImageDescriptor("icons/next.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement next.").open();
		};
	};
	
	private IAction prevAction = new Action("previous", Activator.getImageDescriptor("icons/prev.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement prev.").open();
		};
	};
	
	private IAction copyPathAction = new Action("copy file path") {
		public void run() {
			new MorriganMsgDlg("TODO: implement copy file path.").open();
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	public void updateStatus () {
		if (currentTrack != null) {
			mainLabel.setText("Now playing: " + currentTrack.toString());
			
		} else {
			mainLabel.setText("Idle.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

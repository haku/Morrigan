package net.sparktank.morrigan.views;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.helpers.ClipboardHelper;
import net.sparktank.morrigan.helpers.OrderHelper;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaTrack;
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class ViewPlayer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewPlayer";
	
	private MediaList currentList = null;
	private MediaTrack currentTrack = null;
	private long currentPosition = -1; // In seconds.
	private PlaybackOrder playbackOrder = PlaybackOrder.SEQUENTIAL;
	
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
	
	private MediaTrack getNextTrackToPlay () {
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
		
	};
	
	private Runnable atEndOfTrack = new Runnable() {
		@Override
		public void run() {
			MediaTrack nextTrackToPlay = getNextTrackToPlay();
			if (nextTrackToPlay != null) {
				loadAndStartPlaying(currentList, nextTrackToPlay);
				
			} else {
				currentTrack = null;
				getSite().getShell().getDisplay().asyncExec(updateStatusRunable);
			}
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
	
	private Label mainLabel;
	private OrderSelecter orderSelecter;
	
	private void makeControls (Composite parent) {
		// Main label.
		parent.setLayout(new FillLayout ());
		mainLabel = new Label(parent, SWT.WRAP);
		
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
			if (mainLabel.isDisposed()) return;
			
			if (currentTrack != null && currentList != null) {
				setTitleImage(iconPlay);
				mainLabel.setText(
						"Now playing: " + currentTrack.toString() +
						"\n   From: " + currentList.getListName() +
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
			MediaTrack nextTrackToPlay = getNextTrackToPlay();
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

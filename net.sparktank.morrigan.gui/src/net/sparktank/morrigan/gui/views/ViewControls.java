package net.sparktank.morrigan.gui.views;

import java.util.Collection;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.actions.MinToTrayAction;
import net.sparktank.morrigan.gui.adaptors.ActionListener;
import net.sparktank.morrigan.gui.adaptors.DropMenuListener;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.display.ScreenPainter;
import net.sparktank.morrigan.gui.display.ScreenPainter.ScreenType;
import net.sparktank.morrigan.gui.helpers.RefreshTimer;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.util.TimeHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISizeProvider;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.console.IConsoleConstants;

public class ViewControls extends AbstractPlayerView implements ISizeProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewControls";
	
	final static int SEP = 3;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		makeIcons();
		makeControls(parent);
		
		makeQueueRefresher();
		getPlayer().addQueueChangeListener(this.queueChangedRrefresher);
		
		getEventHandler().updateStatus();
	}
	
	@Override
	public void dispose() {
		getPlayer().removeQueueChangeListener(this.queueChangedRrefresher);
		disposeIcons();
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void setFocus() {
		this.btnPlayPause.setFocus();
	}
	
	@Override
	public Collection<FullScreenAction> getFullScreenActions() {
		return super.getFullScreenActions();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private Image iconPlay;
	private Image iconPause;
	private Image iconStop;
	private Image iconPrev;
	private Image iconNext;
	private Image iconFindInList;
	private Image iconSearch;
	private Image iconScreen;
	private Image iconQueue;
	private Image iconPref;
	
	private int preferedHeight = -1;
	
	private Menu menuOrderMode;
	private Menu menuFullscreen;
	private Menu menuPref;
	
	private Button btnPlayPause;
	private Label lblStatus;
	private Button btnOrderMode;
	Button btnQueue;
	private Canvas videoParent;
	
	private void makeIcons () {
		this.iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		this.iconPause = Activator.getImageDescriptor("icons/pause.gif").createImage();
		this.iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
		this.iconPrev = Activator.getImageDescriptor("icons/prev.gif").createImage();
		this.iconNext = Activator.getImageDescriptor("icons/next.gif").createImage();
		this.iconFindInList = Activator.getImageDescriptor("icons/jumptolist.active.gif").createImage();
		this.iconSearch = Activator.getImageDescriptor("icons/search.gif").createImage();
		this.iconScreen = Activator.getImageDescriptor("icons/display.gif").createImage();
		this.iconQueue = Activator.getImageDescriptor("icons/queue.gif").createImage();
		this.iconPref = Activator.getImageDescriptor("icons/pref.gif").createImage();
	}
	
	private void disposeIcons () {
		this.iconPlay.dispose();
		this.iconPause.dispose();
		this.iconStop.dispose();
		this.iconPrev.dispose();
		this.iconNext.dispose();
		this.iconScreen.dispose();
		this.iconQueue.dispose();
		this.iconPref.dispose();
		this.iconFindInList.dispose();
		this.iconSearch.dispose();
	}
	
	private void makeControls (Composite parent) {
		// Off-screen controls.
		
		MenuManager orderModeMenuMgr = new MenuManager();
		for (final OrderSelectAction a : getOrderMenuActions()) {
			orderModeMenuMgr.add(a);
		}
		this.menuOrderMode = orderModeMenuMgr.createContextMenu(parent);
		
		MenuManager fullscreenMenuMgr = new MenuManager();
		for (final FullScreenAction a : getFullScreenActions()) {
			fullscreenMenuMgr.add(a);
		}
		fullscreenMenuMgr.add(this.showDisplayViewAction);
		this.menuFullscreen = fullscreenMenuMgr.createContextMenu(parent);
		
		MenuManager prefMenuMgr = new MenuManager();
		prefMenuMgr.add(ActionFactory.OPEN_NEW_WINDOW.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new MinToTrayAction(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new Separator());
		MenuManager showViewMenuMgr =  new MenuManager("Show view", "showView");
		showViewMenuMgr.add(new ShowViewAction(ViewMediaExplorer.ID, "Media Explorer", Activator.getImageDescriptor("icons/library.gif")));
		showViewMenuMgr.add(new ShowViewAction(ViewDisplay.ID, "Display", Activator.getImageDescriptor("icons/display.gif")));
		showViewMenuMgr.add(new ShowViewAction(ViewQueue.ID, "Queue", Activator.getImageDescriptor("icons/queue.gif")));
		showViewMenuMgr.add(new ShowViewAction(ViewTagEditor.ID, "Tags", Activator.getImageDescriptor("icons/pref.gif")));
		showViewMenuMgr.add(new ShowViewAction(ViewPicture.ID, "Picture", Activator.getImageDescriptor("icons/pref.gif")));
		showViewMenuMgr.add(new ShowViewAction(IConsoleConstants.ID_CONSOLE_VIEW, "Console", null));
		showViewMenuMgr.add(new Separator());
		showViewMenuMgr.add(ContributionItemFactory.VIEWS_SHORTLIST.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(showViewMenuMgr);
		prefMenuMgr.add(ActionFactory.RESET_PERSPECTIVE.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(ActionFactory.PREFERENCES.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new Separator());
		prefMenuMgr.add(ActionFactory.ABOUT.create(getSite().getWorkbenchWindow()));
		this.menuPref = prefMenuMgr.createContextMenu(parent);
		
		// On-screen controls.
		
		final int seekBarHeight = 10;
		
		parent.setLayout(new FormLayout());
		FormData formData;
		
		Button btnStop = new Button(parent, SWT.PUSH);
		this.btnPlayPause = new Button(parent, SWT.PUSH);
		Button btnPrev = new Button(parent, SWT.PUSH);
		Button btnNext = new Button(parent, SWT.PUSH);
		this.lblStatus = new Label(parent, SWT.NONE);
		Button btnFindInList = new Button(parent, SWT.PUSH);
		Button btnJumpTo = new Button(parent, SWT.PUSH);
		this.videoParent = new Canvas(parent, SWT.NONE);
		this.btnOrderMode = new Button(parent, SWT.PUSH);
		Button btnFullscreen = new Button(parent, SWT.PUSH);
		Button btnPref = new Button(parent, SWT.PUSH);
		this.btnQueue = new Button(parent, SWT.PUSH);
		Canvas seekbar = new Canvas(parent, SWT.NONE);
		
		btnStop.setImage(this.iconStop);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(0, SEP);
		btnStop.setLayoutData(formData);
		btnStop.addSelectionListener(new ActionListener(this.stopAction));
		
		this.btnPlayPause.setImage(this.iconPause);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(btnStop, SEP);
		this.btnPlayPause.setLayoutData(formData);
		this.btnPlayPause.addSelectionListener(new ActionListener(this.pauseAction));
		
		btnPrev.setImage(this.iconPrev);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(this.btnPlayPause, SEP);
		btnPrev.setLayoutData(formData);
		btnPrev.addSelectionListener(new DropMenuListener(btnPrev, getHistoryMenuMgr()));
		
		btnNext.setImage(this.iconNext);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(btnPrev, SEP);
		btnNext.setLayoutData(formData);
		btnNext.addSelectionListener(new ActionListener(this.nextAction));
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(this.lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2 - seekBarHeight/2 - SEP);
		formData.bottom = new FormAttachment(seekbar, -SEP);
		formData.left = new FormAttachment(btnNext, SEP*2);
		formData.right = new FormAttachment(btnFindInList, -SEP);
		this.lblStatus.setLayoutData(formData);
		
		btnFindInList.setImage(this.iconFindInList);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(btnJumpTo, -SEP);
		formData.bottom = new FormAttachment(seekbar, -SEP);
		btnFindInList.setLayoutData(formData);
		btnFindInList.addSelectionListener(new ActionListener(this.findInListAction));
		
		btnJumpTo.setImage(this.iconSearch);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(this.btnOrderMode, -SEP);
		formData.bottom = new FormAttachment(seekbar, -SEP);
		btnJumpTo.setLayoutData(formData);
		btnJumpTo.addSelectionListener(new ActionListener(this.jumpToAction));
		
		this.btnOrderMode.setText(getPlayer().getPlaybackOrder().toString());
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(this.btnQueue, -SEP);
		formData.bottom = new FormAttachment(seekbar, -SEP);
		this.btnOrderMode.setLayoutData(formData);
		this.btnOrderMode.addSelectionListener(new DropMenuListener(this.btnOrderMode, this.menuOrderMode));
		
		this.btnQueue.setImage(this.iconQueue);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(btnPref, -SEP);
		formData.bottom = new FormAttachment(seekbar, -SEP);
		this.btnQueue.setLayoutData(formData);
		this.btnQueue.addSelectionListener(new ActionListener(this.showQueueAction));
		
		btnPref.setImage(this.iconPref);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(btnFullscreen, -SEP);
		btnPref.setLayoutData(formData);
		btnPref.addSelectionListener(new DropMenuListener(btnPref, this.menuPref));
		
		btnFullscreen.setImage(this.iconScreen);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(this.videoParent, -SEP);
		btnFullscreen.setLayoutData(formData);
		btnFullscreen.addSelectionListener(new DropMenuListener(btnFullscreen, this.menuFullscreen));
		
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		formData.width = 80;
		this.videoParent.setLayoutData(formData);
		this.videoParent.setLayout(new FillLayout());
		this.videoParent.addPaintListener(new ScreenPainter(this.videoParent, ScreenType.TINY));
		setLocalMediaFrameParent(this.videoParent);
		
		formData = new FormData();
		formData.top = new FormAttachment(btnStop, SEP);
		formData.left = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(this.videoParent, -SEP);
		formData.height = seekBarHeight;
		seekbar.setLayoutData(formData);
		this.seekbarPainter = new SeekbarPainter((seekbar));
		seekbar.addPaintListener(this.seekbarPainter);
		seekbar.addMouseListener(new SeekbarMouseListener(seekbar));
		
		this.preferedHeight = SEP + btnStop.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + SEP
				+ seekBarHeight + SEP;
	}
	
	@Override
	public int computePreferredSize(boolean width, int availableParallel, int availablePerpendicular, int preferredResult) {
		if (this.preferedHeight > 0 ) {
			return this.preferedHeight;
		}
		
		return 30;
	}
	
	@Override
	public int getSizeFlags(boolean width) {
		return SWT.MAX;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Seekbar.
	
	private SeekbarPainter seekbarPainter = null;
	
	static private class SeekbarPainter implements PaintListener {
		
		private final Canvas canvas;
		private int n = 0;
		private int N = 0;

		public SeekbarPainter (Canvas canvas) {
			this.canvas = canvas;
		}
		
		public void setProgress (int n, int N) {
			this.n = n;
			this.N = N;
			this.canvas.redraw();
		}
		
		@Override
		public void paintControl(PaintEvent e) {
			Rectangle clientArea = this.canvas.getClientArea();
			
			e.gc.setBackground(e.gc.getForeground());
			e.gc.fillRectangle(0, clientArea.height/2 - 1, clientArea.width, 2);
			
			if (this.N > 0) {
				e.gc.fillRectangle(0, 0, (int) ((this.n / (double)this.N) * clientArea.width), clientArea.height - 1);
			}
		}
		
	}
	
	private class SeekbarMouseListener implements MouseListener {
		
		private Canvas canvas;

		public SeekbarMouseListener (Canvas canvas) {
			this.canvas = canvas;
		}
		
		@Override
		public void mouseUp(MouseEvent e) {
			double s = e.x / (double) this.canvas.getClientArea().width;
			if (s >= 0 && s <= 1) {
				getPlayer().seekTo(s);
			}
		}
		
		@Override
		public void mouseDown(MouseEvent e) {/* UNUSED */}
		@Override
		public void mouseDoubleClick(MouseEvent e) {/* UNUSED */}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void updateStatus() {
		if (isDisposed()) return;
		
		String verb;
		
		switch (getPlayer().getPlayState()) {
			case Playing:
				verb = "Playing";
				break;
				
			case Paused:
				verb = "Paused";
				break;
				
			case Loading:
				verb = "Loading";
				break;
				
			case Stopped:
				verb = "Stopped";
				break;
				
			default:
				verb = "Unknown";
				break;
				
		}
		
		if (getPlayer().getCurrentPosition() >= 0) {
			verb = verb + " " + TimeHelper.formatTimeSeconds(getPlayer().getCurrentPosition());
			if (getPlayer().getCurrentTrackDuration() > 0) {
				verb = verb + " of " + TimeHelper.formatTimeSeconds(getPlayer().getCurrentTrackDuration());
				
				this.seekbarPainter.setProgress((int) getPlayer().getCurrentPosition(), getPlayer().getCurrentTrackDuration());
			}
		}
		this.lblStatus.setText(verb + ".");
		
		if (getPlayer().getCurrentItem() != null && getPlayer().getCurrentItem().item != null) {
			getSite().getShell().setText(getPlayer().getCurrentItem().item.toString());
		} else {
			getSite().getShell().setText("Morrigan");
		}
	}
	
	@Override
	protected void orderModeChanged(PlaybackOrder order) {
		this.btnOrderMode.setText(order.toString());
		this.btnOrderMode.getParent().layout();
	}
	
	@Override
	protected void videoParentChanged(Composite newParent) {
		if (this.videoParent.isDisposed()) return;
		
		FormData formData = (FormData) this.videoParent.getLayoutData();
		if (newParent == this.videoParent) {
			formData.width = 80;
		} else {
			formData.width = 0;
		}
		this.videoParent.setLayoutData(formData);
		this.videoParent.setVisible(newParent == this.videoParent);
		this.videoParent.getParent().layout();
	}
	
	Runnable queueChangedRrefresher;
	
	private void makeQueueRefresher () {
		this.queueChangedRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@Override
			public void run() {
				int size = getPlayer().getQueueList().size();
				if (size > 0) {
					ViewControls.this.btnQueue.setText("(" + size + ")");
					
				} else {
					ViewControls.this.btnQueue.setText("");
				}
				ViewControls.this.btnQueue.getParent().layout();
			}
		});
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Display view.
	
	ShowDisplayViewAction showDisplayViewAction = new ShowDisplayViewAction();
	ViewDisplay viewDisplay = null;
	
	public void attachViewDisplay (@SuppressWarnings("hiding") ViewDisplay viewDisplay) {
		this.viewDisplay = viewDisplay;
		viewDisplay.setCloseRunnable(this.onCloseRunnable);
		updateCurrentMediaFrameParent();
		this.showDisplayViewAction.setChecked(true);
	}
	
	public Runnable onCloseRunnable = new Runnable() {
		@Override
		public void run() {
			ViewControls.this.viewDisplay = null;
			updateCurrentMediaFrameParent();
			ViewControls.this.showDisplayViewAction.setChecked(false);
		}
	};
	
	protected class ShowDisplayViewAction extends Action {
		
		public ShowDisplayViewAction () {
			super("Video view", AS_CHECK_BOX);
			this.setImageDescriptor(Activator.getImageDescriptor("icons/display.gif"));
		}
		
		@Override
		public void run() {
			try {
				if (isChecked()) {
					getSite().getPage().showView(ViewDisplay.ID);
				} else {
					getSite().getPage().hideView(ViewControls.this.viewDisplay);
				}
				
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
	@Override
	protected Composite getSecondaryVideoParent() {
		if (this.viewDisplay != null) {
			return this.viewDisplay.getMediaFrameParent();
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

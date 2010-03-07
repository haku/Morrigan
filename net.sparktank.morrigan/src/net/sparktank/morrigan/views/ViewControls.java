package net.sparktank.morrigan.views;


import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.display.ActionListener;
import net.sparktank.morrigan.display.DropMenuListener;
import net.sparktank.morrigan.display.MinToTrayAction;
import net.sparktank.morrigan.display.ScreenPainter;
import net.sparktank.morrigan.display.ScreenPainter.ScreenType;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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

public class ViewControls extends AbstractPlayerView implements ISizeProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewControls";
	
	final static int SEP = 3;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		makeIcons();
		makeControls(parent);
		
		addQueueChangeListener(queueChangedListener);
		
		callUpdateStatus();
	}
	
	@Override
	public void dispose() {
		removeQueueChangeListener(queueChangedListener);
		disposeIcons();
		super.dispose();
	}
	
	@Override
	public void setFocus() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private Image iconPlay;
	private Image iconPause;
	private Image iconStop;
	private Image iconPrev;
	private Image iconNext;
	private Image iconScreen;
	private Image iconQueue;
	private Image iconPref;
	
	private int preferedHeight = -1;
	
	private Menu menuOrderMode;
	private Menu menuFullscreen;
	private Menu menuPref;
	
	private Label lblStatus;
	private Button btnOrderMode;
	private Button btnQueue;
	private Canvas videoParent;
	
	private void makeIcons () {
		iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		iconPause = Activator.getImageDescriptor("icons/pause.gif").createImage();
		iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
		iconPrev = Activator.getImageDescriptor("icons/prev.gif").createImage();
		iconNext = Activator.getImageDescriptor("icons/next.gif").createImage();
		iconScreen = Activator.getImageDescriptor("icons/display.gif").createImage();
		iconQueue = Activator.getImageDescriptor("icons/queue.gif").createImage();
		iconPref = Activator.getImageDescriptor("icons/pref.gif").createImage();
	}
	
	private void disposeIcons () {
		iconPlay.dispose();
		iconPause.dispose();
		iconStop.dispose();
		iconPrev.dispose();
		iconNext.dispose();
		iconScreen.dispose();
		iconQueue.dispose();
		iconPref.dispose();
	}
	
	private void makeControls (Composite parent) {
		// Off-screen controls.
		
		MenuManager orderModeMenuMgr = new MenuManager();
		for (final OrderSelectAction a : getOrderMenuActions()) {
			orderModeMenuMgr.add(a);
		}
		menuOrderMode = orderModeMenuMgr.createContextMenu(parent);
		
		MenuManager fullscreenMenuMgr = new MenuManager();
		for (final FullScreenAction a : getFullScreenActions()) {
			fullscreenMenuMgr.add(a);
		}
		fullscreenMenuMgr.add(showDisplayViewAction);
		menuFullscreen = fullscreenMenuMgr.createContextMenu(parent);
		
		MenuManager prefMenuMgr = new MenuManager();
		prefMenuMgr.add(ActionFactory.OPEN_NEW_WINDOW.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new MinToTrayAction(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new Separator());
		MenuManager showViewMenuMgr =  new MenuManager("Show view", "showView");
		showViewMenuMgr.add(new ShowViewAction(ViewMediaExplorer.ID, "Media Explorer", Activator.getImageDescriptor("icons/library.gif")));
		showViewMenuMgr.add(new Separator());
		showViewMenuMgr.add(ContributionItemFactory.VIEWS_SHORTLIST.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(showViewMenuMgr);
		prefMenuMgr.add(ActionFactory.RESET_PERSPECTIVE.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new Separator());
		prefMenuMgr.add(ActionFactory.PREFERENCES.create(getSite().getWorkbenchWindow()));
		prefMenuMgr.add(new Separator());
		prefMenuMgr.add(ActionFactory.ABOUT.create(getSite().getWorkbenchWindow()));
		menuPref = prefMenuMgr.createContextMenu(parent);
		
		// On-screen controls.
		
		parent.setLayout(new FormLayout());
		
		FormData formData;
		
		Button btnStop = new Button(parent, SWT.PUSH);
		Button btnPlayPause = new Button(parent, SWT.PUSH);
		Button btnPrev = new Button(parent, SWT.PUSH);
		Button btnNext = new Button(parent, SWT.PUSH);
		lblStatus = new Label(parent, SWT.NONE);
		videoParent = new Canvas(parent, SWT.NONE);
		btnOrderMode = new Button(parent, SWT.PUSH);
		Button btnFullscreen = new Button(parent, SWT.PUSH);
		Button btnPref = new Button(parent, SWT.PUSH);
		btnQueue = new Button(parent, SWT.PUSH);
		
		btnStop.setImage(iconStop);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(0, SEP);
		btnStop.setLayoutData(formData);
		btnStop.addSelectionListener(new ActionListener(stopAction));
		
		btnPlayPause.setImage(iconPause);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(btnStop, SEP);
		btnPlayPause.setLayoutData(formData);
		btnPlayPause.addSelectionListener(new ActionListener(pauseAction));
		
		btnPrev.setImage(iconPrev);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(btnPlayPause, SEP);
		btnPrev.setLayoutData(formData);
		btnPrev.addSelectionListener(new DropMenuListener(btnPrev, getHistoryMenuMgr()));
		
		btnNext.setImage(iconNext);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.left = new FormAttachment(btnPrev, SEP);
		btnNext.setLayoutData(formData);
		btnNext.addSelectionListener(new ActionListener(nextAction));
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(btnNext, SEP*2);
		formData.right = new FormAttachment(btnOrderMode, -SEP);
		lblStatus.setLayoutData(formData);
		
		btnOrderMode.setText(getPlaybackOrder().toString());
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(videoParent, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnOrderMode.setLayoutData(formData);
		btnOrderMode.addSelectionListener(new DropMenuListener(btnOrderMode, menuOrderMode));
		
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(btnFullscreen, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		formData.width = 80;
		videoParent.setLayoutData(formData);
		videoParent.setLayout(new FillLayout());
		videoParent.addPaintListener(new ScreenPainter(videoParent, ScreenType.TINY));
		setLocalMediaFrameParent(videoParent);
		
		btnFullscreen.setImage(iconScreen);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(btnQueue, -SEP);
		btnFullscreen.setLayoutData(formData);
		btnFullscreen.addSelectionListener(new DropMenuListener(btnFullscreen, menuFullscreen));
		
		btnQueue.setImage(iconQueue);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(btnPref, -SEP);
		btnQueue.setLayoutData(formData);
		btnQueue.addSelectionListener(new ActionListener(showQueueAction));
		
		btnPref.setImage(iconPref);
		formData = new FormData();
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		btnPref.setLayoutData(formData);
		btnPref.addSelectionListener(new DropMenuListener(btnPref, menuPref));
		
		preferedHeight = SEP + btnStop.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + SEP;
	}
	
	@Override
	public int computePreferredSize(boolean width, int availableParallel, int availablePerpendicular, int preferredResult) {
		if (preferedHeight > 0 ) {
			return preferedHeight;
		} else {
			return 30;
		}
	}
	
	@Override
	public int getSizeFlags(boolean width) {
		return SWT.MAX;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void updateStatus() {
		if (isDisposed()) return;
		
		String verb;
		
		switch (getPlayState()) {
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
		
		if (getCurrentPosition() >= 0) {
			verb = verb + " " + TimeHelper.formatTime(getCurrentPosition());
			if (getCurrentTrackDuration() > 0) {
				verb = verb + " of " + TimeHelper.formatTime(getCurrentTrackDuration());
			}
		}
		lblStatus.setText(verb + ".");
		
		if (getCurrentItem() != null && getCurrentItem().item != null) {
			getSite().getShell().setText(getCurrentItem().item.toString());
		} else {
			getSite().getShell().setText("Morrigan");
		};
	}
	
	@Override
	protected void orderModeChanged(PlaybackOrder order) {
		btnOrderMode.setText(order.toString());
		btnOrderMode.getParent().layout();
	}
	
	@Override
	protected void videoParentChanged(Composite newParent) {
		if (videoParent.isDisposed()) return;
		
		FormData formData = (FormData) videoParent.getLayoutData();
		if (newParent == videoParent) {
			formData.width = 80;
		} else {
			formData.width = 0;
		}
		videoParent.setLayoutData(formData);
		videoParent.setVisible(newParent == videoParent);
		videoParent.getParent().layout();
	}
	
	private volatile boolean queueChangedRunnerScheduled = false;
	
	private Runnable queueChangedListener = new Runnable() {
		@Override
		public void run() {
			if (!queueChangedRunnerScheduled) {
				queueChangedRunnerScheduled = true;
				getSite().getShell().getDisplay().asyncExec(queueChangedRunner);
			}
		}
	};
	
	protected Runnable queueChangedRunner = new Runnable() {
		@Override
		public void run() {
			queueChangedRunnerScheduled = false;
			int size = getQueueList().size();
			if (size > 0) {
				btnQueue.setText("(" + size + ")");
				
			} else {
				btnQueue.setText("");
			}
			btnQueue.getParent().layout();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Display view.
	
	private ShowDisplayViewAction showDisplayViewAction = new ShowDisplayViewAction();
	private ViewDisplay viewDisplay = null;
	
	public void attachViewDisplay (ViewDisplay viewDisplay) throws ImplException {
		this.viewDisplay = viewDisplay;
		viewDisplay.setCloseRunnable(onCloseRunnable);
		updateCurrentMediaFrameParent();
		showDisplayViewAction.setChecked(true);
	}
	
	public Runnable onCloseRunnable = new Runnable() {
		@Override
		public void run() {
			viewDisplay = null;
			updateCurrentMediaFrameParent();
			showDisplayViewAction.setChecked(false);
		}
	};
	
	protected class ShowDisplayViewAction extends Action {
		
		public ShowDisplayViewAction () {
			super("Video view", AS_CHECK_BOX);
			this.setImageDescriptor(Activator.getImageDescriptor("icons/display.gif"));
		}
		
		public void run() {
			try {
				if (isChecked()) {
					getSite().getPage().showView(ViewDisplay.ID);
				} else {
					getSite().getPage().hideView(viewDisplay);
				}
				
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
	@Override
	protected Composite getSecondaryVideoParent() {
		if (viewDisplay != null) {
			return viewDisplay.getMediaFrameParent();
		} else {
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

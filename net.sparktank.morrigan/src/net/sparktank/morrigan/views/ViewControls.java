package net.sparktank.morrigan.views;


import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.display.ScreenPainter;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISizeProvider;

public class ViewControls extends AbstractPlayerView implements ISizeProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewControls";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		makeIcons();
		makeControls(parent);
		
		callUpdateStatus();
	}
	
	@Override
	public void dispose() {
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
	private int preferedHeight = -1;
	
	private Composite parentComposite;
	private Menu orderModeMenu;
	private Menu fullscreenMenu;
	private Button btnOrderMode;
	private Label lblStatus;
	
	private void makeIcons () {
		iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		iconPause = Activator.getImageDescriptor("icons/pause.gif").createImage();
		iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
		iconPrev = Activator.getImageDescriptor("icons/prev.gif").createImage();
		iconNext = Activator.getImageDescriptor("icons/next.gif").createImage();
		iconScreen = Activator.getImageDescriptor("icons/display.gif").createImage();
	}
	
	private void disposeIcons () {
		iconPlay.dispose();
		iconPause.dispose();
		iconStop.dispose();
		iconPrev.dispose();
		iconNext.dispose();
		iconScreen.dispose();
	}
	
	private void makeControls (Composite parent) {
		parentComposite = parent;
		
		// Off-screen controls.
		
		orderModeMenu = new Menu(parent.getShell(), SWT.POP_UP);
		for (final OrderSelectAction a : getOrderMenuActions()) {
			MenuItem menuItem = new MenuItem(orderModeMenu, SWT.PUSH);
			menuItem.setText(a.getText());
			// TODO read-back checked state of action.
			menuItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					a.run();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		
		fullscreenMenu = new Menu(parent.getShell(), SWT.POP_UP);
		for (final FullScreenAction a : getFullScreenActions()) {
			MenuItem menuItem = new MenuItem(fullscreenMenu, SWT.PUSH);
			menuItem.setText(a.getText());
			// TODO read-back checked state of action.
			menuItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					a.run();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		
		
		
		// On-screen controls.
		
		FormLayout layout = new FormLayout();
		parent.setLayout(layout);
		
		final int sep = 3;
		FormData formData;
		
		Button btnStop = new Button(parent, SWT.PUSH);
		Button btnPlayPause = new Button(parent, SWT.PUSH);
		Button btnPrev = new Button(parent, SWT.PUSH);
		Button btnNext = new Button(parent, SWT.PUSH);
		lblStatus = new Label(parent, SWT.NONE);
		Canvas canvas = new Canvas(parent, SWT.NONE);
		btnOrderMode = new Button(parent, SWT.PUSH);
		Button btnFullscreen = new Button(parent, SWT.PUSH);
		
		btnStop.setImage(iconStop);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(0, sep);
		btnStop.setLayoutData(formData);
		btnStop.addSelectionListener(btnStopListener);
		
		btnPlayPause.setImage(iconPause);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(btnStop, sep);
		btnPlayPause.setLayoutData(formData);
		btnPlayPause.addSelectionListener(btnPlayPauseListener);
		
		btnPrev.setImage(iconPrev);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(btnPlayPause, sep);
		btnPrev.setLayoutData(formData);
		btnPrev.addSelectionListener(btnPrevListener);
		
		btnNext.setImage(iconNext);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(btnPrev, sep);
		btnNext.setLayoutData(formData);
		btnNext.addSelectionListener(btnNextListener);
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(btnNext, sep*2);
		formData.right = new FormAttachment(btnOrderMode, -sep);
		lblStatus.setLayoutData(formData);
		
		btnOrderMode.setText(PlaybackOrder.SEQUENTIAL.toString());
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(canvas, -sep);
		btnOrderMode.setLayoutData(formData);
		btnOrderMode.addSelectionListener(btnOrderModeListener);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(btnFullscreen, -sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.width = 80;
		canvas.setLayoutData(formData);
		canvas.setLayout(new FillLayout());
		canvas.addPaintListener(new ScreenPainter(canvas)); // FIXME do we really want this?  Better to hide video space when not needed.
		setMediaFrameParent(canvas);
		
		btnFullscreen.setImage(iconScreen);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(100, -sep);
		btnFullscreen.setLayoutData(formData);
		btnFullscreen.addSelectionListener(btnFullscreenListener);
		
		preferedHeight = sep + btnStop.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + sep;
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
		
		if (getCurrentItem() != null && getCurrentList() != null) {
			switch (internal_getPlayState()) {
				case Playing:
//					setTitleImage(iconPlay);
					break;
					
				case Paused:
//					setTitleImage(iconPause);
					break;
					
				case Loading:
//					setTitleImage(iconPlay); // FIXME new icon?
					break;
					
				case Stopped:
//					setTitleImage(iconStop);
					break;
					
			}
			
			lblStatus.setText(
					"Playing: " + getCurrentPosition() + " : " + getCurrentItem().toString()
			);
			
		} else {
//			setTitleImage(iconStop);
			lblStatus.setText("Idle.");
		};
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Control events.
	
	private SelectionListener btnStopListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			stopAction.run(); // FIXME don't call this like this!
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	private SelectionListener btnPlayPauseListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			pauseAction.run(); // FIXME don't call this like this!
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	private SelectionListener btnPrevListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			prevAction.run(); // FIXME don't call this like this!
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	private SelectionListener btnNextListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			nextAction.run(); // FIXME don't call this like this!
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	// TODO extract common code from these following two methods.
	
	private SelectionListener btnOrderModeListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Rectangle rect = ((Button)e.widget).getBounds();
			Point pt = new Point(rect.x, rect.y + rect.height);
			pt = parentComposite.toDisplay(pt);
			orderModeMenu.setLocation(pt);
			orderModeMenu.setVisible(true);
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	private SelectionListener btnFullscreenListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Rectangle rect = ((Button)e.widget).getBounds();
			Point pt = new Point(rect.x, rect.y + rect.height);
			pt = parentComposite.toDisplay(pt);
			fullscreenMenu.setLocation(pt);
			fullscreenMenu.setVisible(true);
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

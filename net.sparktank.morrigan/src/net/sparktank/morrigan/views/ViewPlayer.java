package net.sparktank.morrigan.views;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.display.ScreenPainter;
import net.sparktank.morrigan.display.ScreenPainter.ScreenType;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;

import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ViewPlayer extends AbstractPlayerView {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewPlayer";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		makeIcons();
		makeControls(parent);
		addToolbar();
		addMenu();
		
		callUpdateStatus();
	}
	
	@Override
	public void setFocus() {
		getLocalMediaFrameParent().setFocus();
	}
	
	@Override
	public void dispose() {
		disposeIcons();
		super.dispose();
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
	
	private void makeControls (Composite parent) {
		// Main label.
		parent.setLayout(new FillLayout());
		parent.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				getLocalMediaFrameParent().setFocus();
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});
		
		Canvas canvas = new Canvas(parent, SWT.NONE);
		canvas.setLayout(new FillLayout());
		canvas.addPaintListener(new ScreenPainter(canvas, ScreenType.MEDIUM));
		setLocalMediaFrameParent(canvas);
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(pauseAction);
		getViewSite().getActionBars().getToolBarManager().add(stopAction);
		getViewSite().getActionBars().getToolBarManager().add(nextAction);
		getViewSite().getActionBars().getToolBarManager().add(new Separator());
	}
	
	private void addMenu () {
		for (OrderSelectAction a : getOrderMenuActions()) {
			getViewSite().getActionBars().getMenuManager().add(a);
		}
		
		getViewSite().getActionBars().getMenuManager().add(new Separator());
		
		getViewSite().getActionBars().getMenuManager().add(pauseAction);
		getViewSite().getActionBars().getMenuManager().add(stopAction);
		getViewSite().getActionBars().getMenuManager().add(prevAction);
		getViewSite().getActionBars().getMenuManager().add(nextAction);
		
		getViewSite().getActionBars().getMenuManager().add(new Separator());
		
		for (FullScreenAction a : getFullScreenActions()) {
			getViewSite().getActionBars().getMenuManager().add(a);
		}
		
		getViewSite().getActionBars().getMenuManager().add(new Separator());
		
		getViewSite().getActionBars().getMenuManager().add(copyPathAction);
	}
	
	@Override
	protected void updateStatus () {
		if (isDisposed()) return;
		
		if (getCurrentItem() != null && getCurrentList() != null) {
			
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
					// "Now playing: " + currentTrack.toString() +
					// "\n   From: " + currentList.getListName() +
					// "\n   Position: " + currentPosition
					
					"Playing: " + getCurrentPosition() + " : " + getCurrentItem().toString()
			);
			
		} else {
			setTitleImage(iconStop);
			setContentDescription("Idle.");
		};
	}
	
	@Override
	protected void orderModeChanged(PlaybackOrder order) {}
	@Override
	protected void videoParentChanged(Composite newParent) {}
	@Override
	protected Composite getSecondaryVideoParent() {
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

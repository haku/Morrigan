package com.vaguehope.morrigan.gui.views;


import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.screen.ScreenPainter;
import com.vaguehope.morrigan.screen.ScreenPainter.ScreenType;

public class ViewPlayer extends AbstractPlayerView {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String ID = "com.vaguehope.morrigan.gui.views.ViewPlayer";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	ScreenPainter screenPainter = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.

	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);

		makeIcons();
		makeControls(parent);
		addToolbar();
		addMenu();

		registerScreenPainter(this.screenPainter);

		callUpdateStatus();
	}

	@Override
	public void setFocus() {
		getLocalMediaFrameParent().setFocus();
	}

	@Override
	public void dispose() {
		if (this.screenPainter != null) {
			unregisterScreenPainter(this.screenPainter);
		}

		disposeIcons();
		super.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.

	private Image iconPlay;
	private Image iconPause;
	private Image iconStop;

	private void makeIcons () {
		this.iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		this.iconPause = Activator.getImageDescriptor("icons/pause.gif").createImage();
		this.iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
	}

	private void disposeIcons () {
		this.iconPlay.dispose();
		this.iconPause.dispose();
		this.iconStop.dispose();
	}

	private void makeControls (final Composite parent) {
		// Main label.
		parent.setLayout(new FillLayout());
		parent.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(final FocusEvent e) {
				getLocalMediaFrameParent().setFocus();
			}
			@Override
			public void focusLost(final FocusEvent e) {/* UNUSED */}
		});

		Canvas canvas = new Canvas(parent, SWT.NONE);
		canvas.setLayout(new FillLayout());
		this.screenPainter = new ScreenPainter(canvas, ScreenType.MEDIUM);
		canvas.addPaintListener(this.screenPainter);
		setLocalMediaFrameParent(canvas);
	}

	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(this.pauseAction);
		getViewSite().getActionBars().getToolBarManager().add(this.stopAction);
		getViewSite().getActionBars().getToolBarManager().add(this.nextAction);
		getViewSite().getActionBars().getToolBarManager().add(new Separator());
	}

	private void addMenu () {
		for (OrderSelectAction a : getOrderMenuActions()) {
			getViewSite().getActionBars().getMenuManager().add(a);
		}

		getViewSite().getActionBars().getMenuManager().add(new Separator());

		getViewSite().getActionBars().getMenuManager().add(this.pauseAction);
		getViewSite().getActionBars().getMenuManager().add(this.stopAction);
		getViewSite().getActionBars().getMenuManager().add(this.nextAction);

		getViewSite().getActionBars().getMenuManager().add(new Separator());

		for (FullScreenAction a : getFullScreenActions()) {
			getViewSite().getActionBars().getMenuManager().add(a);
		}

		getViewSite().getActionBars().getMenuManager().add(new Separator());

		getViewSite().getActionBars().getMenuManager().add(this.copyPathAction);
	}

	@Override
	protected void updateStatus () {
		if (isDisposed()) return;

		switch (getPlayer().getPlayState()) {
			case PLAYING:
				setTitleImage(this.iconPlay);
				break;

			case PAUSED:
				setTitleImage(this.iconPause);
				break;

			case LOADING:
				setTitleImage(this.iconPlay); // FIXME new icon?
				break;

			case STOPPED:
				setTitleImage(this.iconStop);
				break;

			default:
				throw new IllegalArgumentException();

		}

		if (getPlayer().getCurrentItem() != null && getPlayer().getCurrentItem().item != null) {
			setContentDescription("Playing: " + getPlayer().getCurrentPosition() + " : " + getPlayer().getCurrentItem().toString());
		} else {
			setContentDescription("Idle.");
		}
	}

	@Override
	protected void orderModeChanged(final PlaybackOrder order) {/* UNUSED */}
	@Override
	protected void videoParentChanged(final Composite newParent) {/* UNUSED */}
	@Override
	protected Composite getSecondaryVideoParent() {
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

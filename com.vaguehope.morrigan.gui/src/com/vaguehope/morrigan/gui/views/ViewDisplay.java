package com.vaguehope.morrigan.gui.views;

import java.util.Collection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.vaguehope.morrigan.gui.views.AbstractPlayerView.FullScreenAction;
import com.vaguehope.morrigan.screen.ScreenPainter;
import com.vaguehope.morrigan.screen.ScreenPainter.ScreenType;

public class ViewDisplay extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String ID = "com.vaguehope.morrigan.gui.views.ViewDisplay";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.

	private ViewControls viewControls = null;

	@Override
	public void createPartControl(final Composite parent) {
		makeControls(parent);
		getSite().getWorkbenchWindow().addPerspectiveListener(this.perspectiveListener);

		IViewPart findView = getSite().getPage().findView(ViewControls.ID);
		if (findView != null) {
			this.viewControls = (ViewControls) findView;
			this.viewControls.attachViewDisplay(this);
			this.viewControls.registerScreenPainter(this.screenPainter);

			refreshFullscreenActions();
			getViewSite().getActionBars().getMenuManager().add(new Action("Refresh monitors") {
				@Override
				public void run() {
					refreshFullscreenActions();
				}
			});
		}
	}

	@Override
	public void dispose() {
		if (this.viewControls != null) {
			this.viewControls.unregisterScreenPainter(this.screenPainter);
		}

		if (this.onCloseRunnable!=null) {
			this.onCloseRunnable.run();
			this.onCloseRunnable = null;
		}

		getSite().getWorkbenchWindow().removePerspectiveListener(this.perspectiveListener);

		super.dispose();
	}

	@Override
	public void setFocus() {
		this.mediaFrameParent.setFocus();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void refreshFullscreenActions () {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		IContributionItem[] items = tbm.getItems();
		for (int i = items.length - 1; i >= 0; i--) {
			if (FullScreenAction.ID.equals(items[i].getId())) {
				tbm.remove(items[i]);
			}
		}

		Collection<FullScreenAction> fullScreenActions = this.viewControls.getFullScreenActions();
		for (FullScreenAction a : fullScreenActions) {
			tbm.add(a);
		}
		tbm.update(true);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IPerspectiveListener2 perspectiveListener = new IPerspectiveListener2() {

		@Override
		public void perspectiveChanged(final IWorkbenchPage page, final IPerspectiveDescriptor perspective, final IWorkbenchPartReference partRef, final String changeId) {
			if (partRef.getId().equals(ID) && changeId.equals(IWorkbenchPage.CHANGE_VIEW_HIDE)) {
				if (ViewDisplay.this.onCloseRunnable!=null) {
					ViewDisplay.this.onCloseRunnable.run();
					ViewDisplay.this.onCloseRunnable = null;
				}
			}
		}

		@Override
		public void perspectiveChanged(final IWorkbenchPage page, final IPerspectiveDescriptor perspective, final String changeId) {/* UNUSED */}
		@Override
		public void perspectiveActivated(final IWorkbenchPage page, final IPerspectiveDescriptor perspective) {/* UNUSED */}
	};

	Composite mediaFrameParent;
	private ScreenPainter screenPainter;

	private void makeControls (final Composite parent) {
		parent.setLayout(new FillLayout());
		parent.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(final FocusEvent e) {
				ViewDisplay.this.mediaFrameParent.setFocus();
			}
			@Override
			public void focusLost(final FocusEvent e) {/* UNUSED */}
		});

		Canvas canvas = new Canvas(parent, SWT.NONE);
		canvas.setLayout(new FillLayout());
		this.screenPainter = new ScreenPainter(canvas, ScreenType.MEDIUM);
		canvas.addPaintListener(this.screenPainter);
		this.mediaFrameParent = canvas;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Video display related methods.

	Runnable onCloseRunnable;

	public void setCloseRunnable (final Runnable onCloseRunnable) {
		this.onCloseRunnable = onCloseRunnable;

	}

	protected Composite getMediaFrameParent () {
		if (this.mediaFrameParent==null) throw new IllegalAccessError("setMediaFrameParent() has not yet been called.");
		return this.mediaFrameParent;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

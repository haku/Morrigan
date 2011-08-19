package com.vaguehope.morrigan.gui.views;

import java.util.Collection;


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

import com.vaguehope.morrigan.gui.display.ScreenPainter;
import com.vaguehope.morrigan.gui.display.ScreenPainter.ScreenType;
import com.vaguehope.morrigan.gui.views.AbstractPlayerView.FullScreenAction;

public class ViewDisplay extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "com.vaguehope.morrigan.gui.views.ViewDisplay";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	private ViewControls viewControls = null;
	
	@Override
	public void createPartControl(Composite parent) {
		makeControls(parent);
		getSite().getWorkbenchWindow().addPerspectiveListener(this.perspectiveListener);
		
		IViewPart findView = getSite().getPage().findView(ViewControls.ID);
		if (findView!=null) {
			this.viewControls = (ViewControls) findView;
			this.viewControls.attachViewDisplay(this);
			this.viewControls.registerScreenPainter(this.screenPainter);
			
			Collection<FullScreenAction> fullScreenActions = this.viewControls.getFullScreenActions();
			for (FullScreenAction a : fullScreenActions) {
				getViewSite().getActionBars().getToolBarManager().add(a);
			}
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
	
	private IPerspectiveListener2 perspectiveListener = new IPerspectiveListener2() {
		
		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId) {
			if (partRef.getId().equals(ID) && changeId.equals(IWorkbenchPage.CHANGE_VIEW_HIDE)) {
				if (ViewDisplay.this.onCloseRunnable!=null) {
					ViewDisplay.this.onCloseRunnable.run();
					ViewDisplay.this.onCloseRunnable = null;
				}
			}
		}
		
		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {/* UNUSED */}
		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {/* UNUSED */}
	};
	
	Composite mediaFrameParent;
	private ScreenPainter screenPainter;
	
	private void makeControls (Composite parent) {
		parent.setLayout(new FillLayout());
		parent.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				ViewDisplay.this.mediaFrameParent.setFocus();
			}
			@Override
			public void focusLost(FocusEvent e) {/* UNUSED */}
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
	
	public void setCloseRunnable (Runnable onCloseRunnable) {
		this.onCloseRunnable = onCloseRunnable;
		
	}
	
	protected Composite getMediaFrameParent () {
		if (this.mediaFrameParent==null) throw new IllegalAccessError("setMediaFrameParent() has not yet been called.");
		return this.mediaFrameParent;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

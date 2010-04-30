package net.sparktank.morrigan.gui.views;

import java.util.List;

import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.ScreenPainter;
import net.sparktank.morrigan.gui.display.ScreenPainter.ScreenType;
import net.sparktank.morrigan.gui.views.AbstractPlayerView.FullScreenAction;

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

public class ViewDisplay extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewDisplay";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	private ViewControls viewControls = null;
	
	@Override
	public void createPartControl(Composite parent) {
		makeControls(parent);
		getSite().getWorkbenchWindow().addPerspectiveListener(perspectiveListener);
		
		IViewPart findView = getSite().getPage().findView(ViewControls.ID);
		if (findView!=null) {
			viewControls = (ViewControls) findView;
			try {
				viewControls.attachViewDisplay(this);
				viewControls.registerScreenPainter(screenPainter);
				
				List<FullScreenAction> fullScreenActions = viewControls.getFullScreenActions();
				for (FullScreenAction a : fullScreenActions) {
					getViewSite().getActionBars().getToolBarManager().add(a);
				}
				
			} catch (ImplException e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
		}
	}
	
	@Override
	public void dispose() {
		if (viewControls != null) {
			viewControls.unregisterScreenPainter(screenPainter);
		}
		
		if (onCloseRunnable!=null) {
			onCloseRunnable.run();
			onCloseRunnable = null;
		}
		
		getSite().getWorkbenchWindow().removePerspectiveListener(perspectiveListener);
		
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		mediaFrameParent.setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IPerspectiveListener2 perspectiveListener = new IPerspectiveListener2() {
		
		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId) {
			if (partRef.getId().equals(ID) && changeId.equals(IWorkbenchPage.CHANGE_VIEW_HIDE)) {
				if (onCloseRunnable!=null) {
					onCloseRunnable.run();
					onCloseRunnable = null;
				}
			}
		}
		
		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {}
		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Composite mediaFrameParent;
	private ScreenPainter screenPainter;
	
	private void makeControls (Composite parent) {
		parent.setLayout(new FillLayout());
		parent.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				mediaFrameParent.setFocus();
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});
		
		Canvas canvas = new Canvas(parent, SWT.NONE);
		canvas.setLayout(new FillLayout());
		screenPainter = new ScreenPainter(canvas, ScreenType.MEDIUM);
		canvas.addPaintListener(screenPainter);
		mediaFrameParent = canvas;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Video display related methods.
	
	private Runnable onCloseRunnable;
	
	public void setCloseRunnable (Runnable onCloseRunnable) {
		this.onCloseRunnable = onCloseRunnable;
		
	}
	
	protected Composite getMediaFrameParent () {
		if (mediaFrameParent==null) throw new IllegalAccessError("setMediaFrameParent() has not yet been called.");
		return mediaFrameParent;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

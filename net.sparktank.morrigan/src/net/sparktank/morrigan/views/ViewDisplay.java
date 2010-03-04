package net.sparktank.morrigan.views;

import net.sparktank.morrigan.display.ScreenPainter;
import net.sparktank.morrigan.display.ScreenPainter.ScreenType;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class ViewDisplay extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewDisplay";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	@Override
	public void createPartControl(Composite parent) {
		makeControls(parent);
		getSite().getWorkbenchWindow().addPerspectiveListener(perspectiveListener);
	}
	
	@Override
	public void dispose() {
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
				if (onCloseRunnable!=null) onCloseRunnable.run();
			}
		}
		
		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {}
		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Composite mediaFrameParent;
	
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
		canvas.addPaintListener(new ScreenPainter(canvas, ScreenType.MEDIUM));
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

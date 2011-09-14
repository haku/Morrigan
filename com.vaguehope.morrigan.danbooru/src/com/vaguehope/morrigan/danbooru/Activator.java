package com.vaguehope.morrigan.danbooru;

import java.util.LinkedList;
import java.util.List;


import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.gui.views.ViewTagEditor;

public class Activator extends AbstractUIPlugin {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String PLUGIN_ID = "com.vaguehope.morrigan.danbooru"; //$NON-NLS-1$
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// The shared instance
	private static Activator plugin;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Activator () { /* UNUSED */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void start (BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		bootstrap();
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		unstrap();
		
		plugin = null;
		super.stop(context);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Returns the shared instance.
	 */
	public static Activator getDefault () {
		return plugin;
	}
	
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void bootstrap () {
		getWorkbench().addWindowListener(this.windowListener);
		
		for (IWorkbenchWindow window : getWorkbench().getWorkbenchWindows()) {
			window.addPageListener(Activator.this.pageListener);
			
			for (IWorkbenchPage page : window.getPages()) {
				page.addPartListener(this.partListener);
				
				for (IViewReference viewRef : page.getViewReferences()) {
					if (viewRef.getId().equals(ViewTagEditor.ID)) {
						IWorkbenchPart part = viewRef.getPart(false);
						if (part != null) enhancePart(part);
					}
				}
			}
		}
	}
	
	private void unstrap () {
		getWorkbench().removeWindowListener(this.windowListener);
		
		for (IWorkbenchWindow window : getWorkbench().getWorkbenchWindows()) {
			window.removePageListener(Activator.this.pageListener);
			
			for (IWorkbenchPage page : window.getPages()) {
				page.removePartListener(this.partListener);
				
    			for (IViewReference viewRef : page.getViewReferences()) {
    				if (viewRef.getId().equals(ViewTagEditor.ID)) {
    					IWorkbenchPart part = viewRef.getPart(false);
    					if (part != null) dehancePart(part);
    				}
    			}
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IWindowListener windowListener = new IWindowListener() {
		
		@Override
		public void windowOpened(IWorkbenchWindow window) {
			window.addPageListener(Activator.this.pageListener);
		}
		
		@Override
		public void windowClosed(IWorkbenchWindow window) {
			window.removePageListener(Activator.this.pageListener);
		}
		
		@Override
		public void windowActivated(IWorkbenchWindow window) {/* UNUSED */}
		@Override
		public void windowDeactivated(IWorkbenchWindow window) {/* UNUSED */}
	};
	
	IPageListener pageListener = new IPageListener() {
		
		@Override
		public void pageOpened(IWorkbenchPage page) {
			page.addPartListener(Activator.this.partListener);
		}
		
		@Override
		public void pageClosed(IWorkbenchPage page) {
			page.removePartListener(Activator.this.partListener);
		}
		
		@Override
		public void pageActivated(IWorkbenchPage page) { /* UNUSED */}
	};
	
	IPartListener partListener = new IPartListener() {
		
		@Override
		public void partOpened(IWorkbenchPart part) {
			enhancePart(part);
		}
		
		@Override
		public void partActivated (IWorkbenchPart part) {
			enhancePart(part);
		}
		
		@Override
		public void partClosed(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partDeactivated(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partBroughtToTop(IWorkbenchPart part) {/* UNUSED */}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	void enhancePart (IWorkbenchPart part) {
		if (part instanceof ViewTagEditor) {
			ViewTagEditor tagEd = (ViewTagEditor) part;
			enhanceTagEditor(tagEd);
		}
	}
	
	void dehancePart (IWorkbenchPart part) {
		if (part instanceof ViewTagEditor) {
			ViewTagEditor tagEd = (ViewTagEditor) part;
			dehanceTagEditor(tagEd);
		}
	}
	
	void enhanceTagEditor (final ViewTagEditor tagEd) {
		IToolBarManager toolBarManager = tagEd.getViewSite().getActionBars().getToolBarManager();
		
		boolean found = false;
		for (IContributionItem item : toolBarManager.getItems()) {
			if (item.getId().equals(GetDanbooruTagsAction.ID)) {
				found = true;
				break;
			}
		}
		
		if (!found) {
			toolBarManager.add(new GetDanbooruTagsAction(tagEd));
			
			this.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					tagEd.getViewSite().getActionBars().updateActionBars();
				}
			});
		}
	}
	
	void dehanceTagEditor (final ViewTagEditor tagEd) {
		final IToolBarManager toolBarManager = tagEd.getViewSite().getActionBars().getToolBarManager();
		
		List<IContributionItem> items = new LinkedList<IContributionItem>();
		for (IContributionItem item : toolBarManager.getItems()) {
			if (item.getId().equals(GetDanbooruTagsAction.ID)) {
				items.add(item);
			}
		}
		for (final IContributionItem item : items) {
			this.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					toolBarManager.remove(item);
					tagEd.getViewSite().getActionBars().updateActionBars();
				}
			});
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

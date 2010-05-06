package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;

import net.sparktank.morrigan.gui.actions.NewLibraryAction;
import net.sparktank.morrigan.gui.actions.NewPlaylistAction;
import net.sparktank.morrigan.gui.actions.NewRemoteLibraryAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.CallMediaListEditor;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.library.LibraryHelper;
import net.sparktank.morrigan.model.library.RemoteLibraryHelper;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

public class ViewMediaExplorer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewMediaExplorer";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private TableViewer viewer = null;
	
	ArrayList<MediaExplorerItem> items = new ArrayList<MediaExplorerItem>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This is a callback that will allow us to create the viewer and initialise it.
	 */
	public void createPartControl(Composite parent) {
		makeContent();
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(viewer);
		viewer.addDoubleClickListener(doubleClickListener);
		
		addToolbar();
	}
	
	@Override
	public void dispose() {
		imageCache.clearCache();
		super.dispose();
	}
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return items.toArray();
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
		@Override
		public void dispose() {}
		
	};
	
	private ILabelProvider labelProvider = new ILabelProvider() {
		
		@Override
		public String getText(Object element) {
			if (element instanceof MediaExplorerItem) {
				MediaExplorerItem item = (MediaExplorerItem) element;
				return item.toString();
			}
			return null;
		}
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof MediaExplorerItem) {
				MediaExplorerItem item = (MediaExplorerItem) element;
				switch (item.type) {
					
					case PLAYLIST:
						return imageCache.readImage("icons/playlist.gif");
						
					case LIBRARY:
						return imageCache.readImage("icons/library.gif");
						
					case DISPLAY:
						return imageCache.readImage("icons/display.gif");
					
					case REMOTELIBRARY:
						return imageCache.readImage("icons/library-remote.gif");
				}
			}
			return null;
		}
		
		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		
		@Override
		public void removeListener(ILabelProviderListener listener) {}
		@Override
		public void dispose() {}
		@Override
		public void addListener(ILabelProviderListener listener) {}
	};
	
	private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent event) {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(CallMediaListEditor.ID, null);
			} catch (CommandException e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(new NewLibraryAction(getViewSite().getWorkbenchWindow()));
		getViewSite().getActionBars().getToolBarManager().add(new NewRemoteLibraryAction(getViewSite().getWorkbenchWindow()));
		getViewSite().getActionBars().getToolBarManager().add(new NewPlaylistAction(getViewSite().getWorkbenchWindow()));
	}
	
	private void makeContent () {
		items.clear();
		items.addAll(LibraryHelper.getAllLibraries());
		items.addAll(RemoteLibraryHelper.getAllRemoteLibraries());
		items.addAll(PlaylistHelper.getAllPlaylists());
	}
	
	private ImageCache imageCache = new ImageCache();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	public void refresh () {
		makeContent();
		viewer.refresh();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
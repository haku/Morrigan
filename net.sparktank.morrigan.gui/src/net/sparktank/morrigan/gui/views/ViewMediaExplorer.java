package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;

import net.sparktank.morrigan.gui.actions.NewLibraryAction;
import net.sparktank.morrigan.gui.actions.NewMixedDbAction;
import net.sparktank.morrigan.gui.actions.NewPlaylistAction;
import net.sparktank.morrigan.gui.actions.NewRemoteLibraryAction;
import net.sparktank.morrigan.gui.actions.NewRemoteMixedDbAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.CallMediaListEditor;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDbHelper;
import net.sparktank.morrigan.model.pictures.gallery.LocalGalleryHelper;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.tracks.library.remote.RemoteLibraryHelper;
import net.sparktank.morrigan.model.tracks.playlist.PlaylistHelper;

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
	
	private TableViewer tableViewer = null;
	
	ArrayList<MediaExplorerItem> items = new ArrayList<MediaExplorerItem>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This is a callback that will allow us to create the viewer and initialise it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		makeContent();
		
		this.tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		this.tableViewer.setContentProvider(this.contentProvider);
		this.tableViewer.setLabelProvider(this.labelProvider);
		this.tableViewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(this.tableViewer);
		this.tableViewer.addDoubleClickListener(this.doubleClickListener);
		
		addToolbar();
	}
	
	@Override
	public void dispose() {
		this.imageCache.clearCache();
		super.dispose();
	}
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return ViewMediaExplorer.this.items.toArray();
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
		
		@Override
		public void dispose() {/* UNUSED */}
		
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
						return ViewMediaExplorer.this.imageCache.readImage("icons/playlist.gif");
						
					case LIBRARY:
						return ViewMediaExplorer.this.imageCache.readImage("icons/library.gif");
						
					case DISPLAY:
						return ViewMediaExplorer.this.imageCache.readImage("icons/display.gif");
					
					case REMOTELIBRARY:
						return ViewMediaExplorer.this.imageCache.readImage("icons/library-remote.gif");
						
					case  LOCALGALLERY:
						return ViewMediaExplorer.this.imageCache.readImage("icons/library.gif"); // TODO choose icon.
						
					case LOCALMMDB:
						return ViewMediaExplorer.this.imageCache.readImage("icons/library.gif"); // TODO choose icon.
						
				}
			}
			return null;
		}
		
		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		
		@Override
		public void removeListener(ILabelProviderListener listener) {/* UNUSED */}
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void addListener(ILabelProviderListener listener) {/* UNUSED */}
	};
	
	private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		@Override
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
//		getViewSite().getActionBars().getToolBarManager().add(new NewGalleryAction(getViewSite().getWorkbenchWindow()));
		getViewSite().getActionBars().getToolBarManager().add(new NewMixedDbAction(getViewSite().getWorkbenchWindow()));
		getViewSite().getActionBars().getToolBarManager().add(new NewRemoteMixedDbAction(getViewSite().getWorkbenchWindow()));
	}
	
	private void makeContent () {
		this.items.clear();
		this.items.addAll(LocalMixedMediaDbHelper.getAllMmdb());
		this.items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
		this.items.addAll(LocalLibraryHelper.getAllLibraries());
		this.items.addAll(LocalGalleryHelper.getAllGalleries());
		this.items.addAll(RemoteLibraryHelper.getAllRemoteLibraries());
		this.items.addAll(PlaylistHelper.getAllPlaylists());
	}
	
	ImageCache imageCache = new ImageCache();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		this.tableViewer.getControl().setFocus();
	}
	
	public void refresh () {
		makeContent();
		this.tableViewer.refresh();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
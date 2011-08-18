package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.Iterator;

import net.sparktank.morrigan.gui.actions.DbUpdateAction;
import net.sparktank.morrigan.gui.actions.NewMixedDbAction;
import net.sparktank.morrigan.gui.actions.NewRemoteMixedDbAction;
import net.sparktank.morrigan.gui.adaptors.MediaExplorerItemLblProv;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.MediaItemDbEditorInput;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.gui.handler.CallMediaListEditor;
import net.sparktank.morrigan.gui.handler.CallPlayMedia;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.MediaListReference.MediaListType;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.server.model.RemoteMixedMediaDb;
import net.sparktank.morrigan.server.model.RemoteMixedMediaDbHelper;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

public class ViewMediaExplorer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewMediaExplorer";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private volatile boolean isDisposed = false;
	TableViewer tableViewer = null;
	ArrayList<MediaListReference> items = new ArrayList<MediaListReference>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This is a callback that will allow us to create the viewer and initialise it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		makeContent();
		
		this.tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		this.tableViewer.setContentProvider(this.contentProvider);
		this.tableViewer.setLabelProvider(new MediaExplorerItemLblProv(this.imageCache));
		this.tableViewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(this.tableViewer);
		this.tableViewer.addDoubleClickListener(this.doubleClickListener);
		
		addToolbar();
		makeContextMenu();
	}
	
	@Override
	public void dispose() {
		this.isDisposed = true;
		this.imageCache.clearCache();
		super.dispose();
	}
	
	protected boolean isDisposed () {
		return this.isDisposed;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
	private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		@Override
		public void doubleClick(DoubleClickEvent event) {
			ViewMediaExplorer.this.openAction.run();
		}
	};
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(new NewMixedDbAction(getViewSite().getWorkbenchWindow()));
		getViewSite().getActionBars().getToolBarManager().add(new NewRemoteMixedDbAction(getViewSite().getWorkbenchWindow()));
	}
	
	private void makeContextMenu () {
		final MenuManager mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				final IStructuredSelection selection = (IStructuredSelection) ViewMediaExplorer.this.tableViewer.getSelection();
				if (!selection.isEmpty() && selection.size() == 1) {
					Object selectedObject = selection.getFirstElement();
					if (selectedObject != null && selectedObject instanceof MediaListReference) {
						mgr.add(ViewMediaExplorer.this.openAction);
						
						MediaListReference item = (MediaListReference) selectedObject;
						
						if (item.getType() == MediaListReference.MediaListType.LOCALMMDB) {
							mgr.add(new OpenViewAction(item));
							
							mgr.add(new PlayAction(item, false));
							mgr.add(new PlayAction(item, true));
						}
						
						mgr.add(new Separator());
						
						if (item.getType() == MediaListReference.MediaListType.LOCALMMDB || item.getType() == MediaListReference.MediaListType.REMOTEMMDB) {
							mgr.add(new MediaExplorerItemUpdateAction(item));
						}
						
						if (item.getType() == MediaListReference.MediaListType.LOCALMMDB) {
							mgr.add(new DbPropertiesAction(item));
						}
					}
				}
				else if (selection.size() == 2) {
					Iterator<?> iterator = selection.iterator();
					Object o1 = iterator.next();
					Object o2 = iterator.next();
					if (o1 instanceof MediaListReference && o2 instanceof MediaListReference) {
						MediaListReference r1 = (MediaListReference) o1;
						MediaListReference r2 = (MediaListReference) o2;
						
						MediaListReference local = null;
						MediaListReference remote = null;
						
						if (r1.getType() == MediaListType.LOCALMMDB) {
							local = r1;
						}
						else if (r2.getType() == MediaListType.LOCALMMDB) {
							local = r2;
						}
						
						if (r1.getType() == MediaListType.REMOTEMMDB) {
							remote = r1;
						}
						else if (r2.getType() == MediaListType.REMOTEMMDB) {
							remote = r2;
						}
						
						if (local != null && remote != null) {
							mgr.add(new SyncMetadataAction(local, remote));
						}
					}
				}
			}
		});
		this.tableViewer.getControl().setMenu(mgr.createContextMenu(this.tableViewer.getControl()));
	}
	
	private void makeContent () {
		this.items.clear();
		this.items.addAll(MediaFactoryImpl.get().getAllLocalMixedMediaDbs());
		this.items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
	}
	
	ImageCache imageCache = new ImageCache();
	
	void updateStatus () {
		if (isDisposed()) return ;
		
		if (this.items.size() == 0) {
			setContentDescription("Click the DB icon to create a new DB.");
		}
		else {
			setContentDescription(this.items.size() + " items.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	protected IAction openAction = new Action("Open") {
		@Override
		public void run() {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(CallMediaListEditor.ID, null);
			} catch (CommandException e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
	class OpenViewAction extends Action {
		
		private final MediaListReference mediaExplorerItem;

		public OpenViewAction (MediaListReference mediaExplorerItem) {
			super("Open view... ");
			this.mediaExplorerItem = mediaExplorerItem;
		}
		
		@Override
		public void run() {
			try {
				if (this.mediaExplorerItem.getType() == MediaListReference.MediaListType.LOCALMMDB) {
					InputDialog dlg = new InputDialog(
							Display.getCurrent().getActiveShell(),
							"Open view", "Filter string:", "", null);
					if (dlg.open() == Window.OK) {
						String filter = dlg.getValue();
						if (filter != null && filter.length() > 0) {
							MediaItemDbEditorInput input = EditorFactory.getMmdbInput(this.mediaExplorerItem.getIdentifier(), filter);
							IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
							page.openEditor(input, LocalMixedMediaDbEditor.ID);
						}
					}
				}
				else {
					throw new IllegalArgumentException("Views are not supported on MediaExplorerItem of type '"+this.mediaExplorerItem.getType()+"'.");
				}
			}
			catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
	static class MediaExplorerItemUpdateAction extends Action {
		
		private final MediaListReference mediaExplorerItem;

		public MediaExplorerItemUpdateAction (MediaListReference mediaExplorerItem) {
			super("Update " + mediaExplorerItem.getTitle());
			this.mediaExplorerItem = mediaExplorerItem;
		}
		
		@Override
		public void run() {
			try {
				if (this.mediaExplorerItem.getType() == MediaListReference.MediaListType.LOCALMMDB) {
					ILocalMixedMediaDb l = MediaFactoryImpl.get().getLocalMixedMediaDb(this.mediaExplorerItem.getIdentifier());
					new DbUpdateAction(l).run();
				}
				else if (this.mediaExplorerItem.getType() == MediaListReference.MediaListType.REMOTEMMDB) {
					IRemoteMixedMediaDb l = RemoteMixedMediaDb.FACTORY.manufacture(this.mediaExplorerItem.getIdentifier());
					new DbUpdateAction(l).run();
				}
				else {
					throw new IllegalArgumentException("Unable to start update for MediaExplorerItem of type '"+this.mediaExplorerItem.getType()+"'.");
				}
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
	class PlayAction extends Action {
		
		private final MediaListReference mediaExplorerItem;
		private final boolean addToQueue;
		
		public PlayAction (MediaListReference mediaExplorerItem, boolean addToQueue) {
			super((addToQueue ? "Enqueue " : "Play ") + mediaExplorerItem.getTitle());
			this.mediaExplorerItem = mediaExplorerItem;
			this.addToQueue = addToQueue;
		}
		
		@Override
		public void run() {
			try {
				if (this.mediaExplorerItem.getType() == MediaListReference.MediaListType.LOCALMMDB) {
					ILocalMixedMediaDb l = MediaFactoryImpl.get().getLocalMixedMediaDb(this.mediaExplorerItem.getIdentifier());
					l.read();
					CallPlayMedia.playItem(getSite().getWorkbenchWindow().getActivePage(), l, this.addToQueue);
				}
				else if (this.mediaExplorerItem.getType() == MediaListReference.MediaListType.REMOTEMMDB) {
					new MorriganMsgDlg("TODO: Implement play for RMMDBs.").open();
				}
				else {
					throw new IllegalArgumentException("Unable to play MediaExplorerItem of type '"+this.mediaExplorerItem.getType()+"'.");
				}
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
	class DbPropertiesAction extends Action {
		
		private final MediaListReference mediaExplorerItem;

		public DbPropertiesAction (MediaListReference mediaExplorerItem) {
			super("Properties for " + mediaExplorerItem.getTitle());
			this.mediaExplorerItem = mediaExplorerItem;
		}
		
		@Override
		public void run() {
			try {
				if (this.mediaExplorerItem.getType() == MediaListReference.MediaListType.LOCALMMDB) {
					ILocalMixedMediaDb l = MediaFactoryImpl.get().getLocalMixedMediaDb(this.mediaExplorerItem.getIdentifier());
					IViewPart showView = getSite().getPage().showView(ViewLibraryProperties.ID);
					ViewLibraryProperties viewProp = (ViewLibraryProperties) showView;
					viewProp.setContent(l);
				}
				else {
					throw new IllegalArgumentException("Unable to show properties for MediaExplorerItem of type '"+this.mediaExplorerItem.getType()+"'.");
				}
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
	static class SyncMetadataAction extends Action {
		
		private final MediaListReference local;
		private final MediaListReference remote;

		public SyncMetadataAction (MediaListReference local, MediaListReference remote) {
			super("Sync metadata '"+local+"' x '"+remote+"' desu~");
			this.local = local;
			this.remote = remote;
		}
		
		@Override
		public void run() {
			try {
    			ILocalMixedMediaDb localDb = MediaFactoryImpl.get().getLocalMixedMediaDb(this.local.getIdentifier());
    			IRemoteMixedMediaDb remoteDb = RemoteMixedMediaDb.FACTORY.manufacture(this.remote.getIdentifier());
    			IMorriganTask task = MediaFactoryImpl.get().getSyncMetadataRemoteToLocalTask(localDb, remoteDb);
    			TaskJob job = new TaskJob(task, Display.getCurrent());
    			job.schedule();
			}
			catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
	}
	
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
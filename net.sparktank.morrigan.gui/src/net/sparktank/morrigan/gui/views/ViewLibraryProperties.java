package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.actions.DbUpdateAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.gui.editors.tracks.LocalLibraryEditor;
import net.sparktank.morrigan.model.media.impl.MediaItemDb;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class ViewLibraryProperties extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewLibraryProperties";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		addToolbar();
		
		getViewSite().getPage().addPartListener(this.partListener);
		
		this.listChange.run();
	}
	
	@Override
	public void setFocus() {
		this.tableViewer.getTable().setFocus();
	}
	
	@Override
	public void dispose() {
		getViewSite().getPage().removePartListener(this.partListener);
		setContent(null, false);
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IPartListener partListener = new IPartListener() {
		@Override
		public void partActivated(IWorkbenchPart part) {
			MediaItemDb<?,?,?> ml = null;
			
			/*
			 * At the moment this checks the supported editors directly.
			 * TODO Is there a good way to make this more abstract?
			 */
			if (part instanceof LocalLibraryEditor) {
				LocalLibraryEditor e = (LocalLibraryEditor) part;
				ml = e.getMediaList();
			}
			else if (part instanceof LocalMixedMediaDbEditor) {
				LocalMixedMediaDbEditor e = (LocalMixedMediaDbEditor) part;
				ml = e.getMediaList();
			}
			
			if (ml != null) {
				setContent(ml);
			}
		}
		
		@Override
		public void partOpened(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partDeactivated(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partClosed(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partBroughtToTop(IWorkbenchPart part) {/* UNUSED */}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MediaItemDb<?,?,?> library;
	DbUpdateAction dbUpdateAction = new DbUpdateAction();
	
	public void setContent (MediaItemDb<?,?,?> library) {
		setContent(library, true);
	}
	
	public void setContent (MediaItemDb<?,?,?> library, boolean updateGui) {
		if (this.library == library) return;
		
		if (this.library!=null) {
			this.library.removeChangeEvent(this.listChange);
		}
		
		this.library = library;
		
		if (this.library!=null) {
			this.library.addChangeEvent(this.listChange);
		}
		
		this.dbUpdateAction.setMediaDb(this.library);
		
		if (updateGui) this.listChange.run();
	}
	
	public void showAddDlg (boolean promptScan) {
		new AddAction().run(promptScan);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		this.tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		this.tableViewer.setContentProvider(this.sourcesProvider);
		this.tableViewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(this.tableViewer);
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(new AddAction());
		getViewSite().getActionBars().getToolBarManager().add(this.removeAction);
		getViewSite().getActionBars().getToolBarManager().add(this.dbUpdateAction);
	}
	
	private IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			List<String> sources = null;
			
			if (ViewLibraryProperties.this.library!=null) {
				try {
					sources = ViewLibraryProperties.this.library.getSources();
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				
				if (sources != null) {
					return sources.toArray();
				}
				
				return new String[]{};
				
			}
			
			return new String[]{};
		}
		
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
		
	};
	
	ArrayList<String> getSelectedSources () {
		ISelection selection = this.tableViewer.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			ArrayList<String> ret = new ArrayList<String>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof String) {
						String item = (String) selectedObject;
						ret.add(item);
					}
				}
			}
			return ret;
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	volatile boolean updateGuiRunableScheduled = false;
	
	Runnable listChange = new Runnable() {
		@Override
		public void run() {
			if (!ViewLibraryProperties.this.updateGuiRunableScheduled) {
				ViewLibraryProperties.this.updateGuiRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(ViewLibraryProperties.this.updateGuiRunable);
			}
		}
	};
	
	Runnable updateGuiRunable = new Runnable() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			ViewLibraryProperties.this.updateGuiRunableScheduled = false;
			if (ViewLibraryProperties.this.tableViewer.getTable().isDisposed()) return;
			
			if (ViewLibraryProperties.this.library!=null) {
				
				int numSources = -1;
				try {
					numSources = ViewLibraryProperties.this.library.getSources().size();
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				
				setContentDescription(
						ViewLibraryProperties.this.library.getListName() + " contains " + ViewLibraryProperties.this.library.getCount()
						+ " items from " + numSources + " sources."
				);
				
			} else {
				setContentDescription("No library selected.");
			}
			
			ViewLibraryProperties.this.tableViewer.refresh();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private class AddAction extends Action {
		
		private String lastDir = null;
		
		public AddAction () {
			super("add", Activator.getImageDescriptor("icons/plus.gif"));
		}
		
		@Override
		public void run() {
			run(false);
		}
		
		public void run(boolean promptScan) {
			if (ViewLibraryProperties.this.library==null) {
				new MorriganMsgDlg("No library selected desu~.").open();
				return;
			}
			
			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Add Source");
			dlg.setMessage("Select a directory containing media.");
			if (this.lastDir != null) {
				dlg.setFilterPath(this.lastDir);
			}
			
			String dir = dlg.open();
			
			if (dir != null) {
				this.lastDir = dir;
				
				try {
					ViewLibraryProperties.this.library.addSource(dir);
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				ViewLibraryProperties.this.listChange.run();
				
				if (promptScan) {
					MorriganMsgDlg dlg2 = new MorriganMsgDlg("Run scan on " + ViewLibraryProperties.this.library.getListName() + " now?", MorriganMsgDlg.YESNO);
					dlg2.open();
					if (dlg2.getReturnCode() == Window.OK) {
						ViewLibraryProperties.this.dbUpdateAction.run();
					}
				}
			}
		}
		
	}
	
	private IAction removeAction = new Action("remove", Activator.getImageDescriptor("icons/minus.gif")) {
		@Override
		public void run() {
			if (ViewLibraryProperties.this.library==null) {
				new MorriganMsgDlg("No library selected desu~.").open();
				return;
			}
			
			ArrayList<String> selectedSources = getSelectedSources();
			if (selectedSources==null || selectedSources.isEmpty()) {
				new MorriganMsgDlg("No items selected desu~.").open();
				return;
			}
			
			MorriganMsgDlg dlg = new MorriganMsgDlg("Remove selected from " + ViewLibraryProperties.this.library.getListName() + "?", MorriganMsgDlg.YESNO);
			dlg.open();
			if (dlg.getReturnCode() == Window.OK) {
				for (String item : selectedSources) {
					try {
						ViewLibraryProperties.this.library.removeSource(item);
					} catch (DbException e) {
						new MorriganMsgDlg(e).open();
					}
					ViewLibraryProperties.this.listChange.run();
				}
			}
			
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

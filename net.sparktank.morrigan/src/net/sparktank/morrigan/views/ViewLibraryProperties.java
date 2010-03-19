package net.sparktank.morrigan.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.editors.LibraryEditor;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.LibraryUpdateAction;
import net.sparktank.morrigan.model.media.MediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class ViewLibraryProperties extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewLibraryProperties";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		addToolbar();
		
		getViewSite().getPage().addPartListener(partListener);
		
		listChange.run();
	}
	
	@Override
	public void setFocus() {}
	
	@Override
	public void dispose() {
		getViewSite().getPage().removePartListener(partListener);
		setContent(null, false);
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IPartListener partListener = new IPartListener() {
		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof LibraryEditor) {
				LibraryEditor libEditor = (LibraryEditor) part;
				setContent(libEditor.getMediaList());
			}
		}
		
		@Override
		public void partOpened(IWorkbenchPart part) {}
		@Override
		public void partDeactivated(IWorkbenchPart part) {}
		@Override
		public void partClosed(IWorkbenchPart part) {}
		@Override
		public void partBroughtToTop(IWorkbenchPart part) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaLibrary library;
	private LibraryUpdateAction libraryUpdateAction = new LibraryUpdateAction();
	
	public void setContent (MediaLibrary library) {
		setContent(library, true);
	}
	
	public void setContent (MediaLibrary library, boolean updateGui) {
		if (this.library == library) return;
		
		if (this.library!=null) {
			this.library.removeChangeEvent(listChange);
		}
		
		this.library = library;
		
		if (this.library!=null) {
			this.library.addChangeEvent(listChange);
		}
		
		libraryUpdateAction.setMediaLibrary(this.library);
		
		if (updateGui) listChange.run();
	}
	
	public void showAddDlg (boolean promptScan) {
		new AddAction().run(promptScan);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL);
		tableViewer.setContentProvider(sourcesProvider);
		tableViewer.setInput(getViewSite()); // use content provider.
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(new AddAction());
		getViewSite().getActionBars().getToolBarManager().add(removeAction);
		getViewSite().getActionBars().getToolBarManager().add(libraryUpdateAction);
	}
	
	private IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			List<String> sources = null;
			
			if (library!=null) {
				try {
					sources = library.getSources();
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				
				return sources.toArray();
				
			} else {
				return new String[]{};
			}
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	private ArrayList<String> getSelectedSources () {
		ISelection selection = tableViewer.getSelection();
		
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
	
	private volatile boolean updateGuiRunableScheduled = false;
	
	private Runnable listChange = new Runnable() {
		@Override
		public void run() {
			if (!updateGuiRunableScheduled) {
				updateGuiRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(updateGuiRunable);
			}
		}
	};
	
	private Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			updateGuiRunableScheduled = false;
			if (tableViewer.getTable().isDisposed()) return;
			
			if (library!=null) {
				
				int numSources = -1;
				try {
					numSources = library.getSources().size();
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				
				setContentDescription(
						library.getListName() + " contains " + library.getCount()
						+ " items from " + numSources + " sources."
				);
				
			} else {
				setContentDescription("No library selected.");
			}
			
			tableViewer.refresh();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private class AddAction extends Action {
		
		private String lastDir = null;
		
		public AddAction () {
			super("add", Activator.getImageDescriptor("icons/plus.gif"));
		}
		
		public void run() {
			run(false);
		}
		
		public void run(boolean promptScan) {
			if (library==null) {
				new MorriganMsgDlg("No library selected desu~.").open();
				return;
			}
			
			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Add Source");
			dlg.setMessage("Select a directory containing media.");
			if (lastDir != null) {
				dlg.setFilterPath(lastDir);
			}
			
			String dir = dlg.open();
			
			if (dir != null) {
				lastDir = dir;
				
				try {
					library.addSource(dir);
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				listChange.run();
				
				if (promptScan) {
					MorriganMsgDlg dlg2 = new MorriganMsgDlg("Run scan on " + library.getListName() + " now?", MorriganMsgDlg.YESNO);
					dlg2.open();
					if (dlg2.getReturnCode() == MorriganMsgDlg.OK) {
						libraryUpdateAction.run();
					}
				}
			}
		}
		
	}
	
	private IAction removeAction = new Action("remove", Activator.getImageDescriptor("icons/minus.gif")) {
		public void run() {
			if (library==null) {
				new MorriganMsgDlg("No library selected desu~.").open();
				return;
			}
			
			ArrayList<String> selectedSources = getSelectedSources();
			if (selectedSources==null || selectedSources.isEmpty()) {
				new MorriganMsgDlg("No items selected desu~.").open();
				return;
			}
			
			MorriganMsgDlg dlg = new MorriganMsgDlg("Remove selected from " + library.getListName() + "?", MorriganMsgDlg.YESNO);
			dlg.open();
			if (dlg.getReturnCode() == MorriganMsgDlg.OK) {
				for (String item : selectedSources) {
					try {
						library.removeSource(item);
					} catch (DbException e) {
						new MorriganMsgDlg(e).open();
					}
					listChange.run();
				}
			}
			
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

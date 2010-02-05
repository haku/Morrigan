package net.sparktank.morrigan.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.library.LibraryUpdateTask;
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
import org.eclipse.ui.part.ViewPart;

public class ViewLibraryProperties extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewLibraryProperties";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		addToolbar();
		updateGui();
	}
	
	@Override
	public void setFocus() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaLibrary library;
	
	public void setContent (MediaLibrary library) {
		this.library = library;
		updateGui();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL);
		tableViewer.setContentProvider(sourcesProvider);
		tableViewer.setInput(getViewSite()); // use content provider.
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(addAction);
		getViewSite().getActionBars().getToolBarManager().add(removeAction);
		getViewSite().getActionBars().getToolBarManager().add(updateAction);
	}
	
	private void updateGui () {
		if (library!=null) {
			setContentDescription( "Sources for " + library.getListName() + ".");
			
		} else {
			setContentDescription("No library selected.");
		}
		
		tableViewer.refresh();
	}
	
	private IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			List<String> sources = null;
			
			if (library!=null) {
				try {
					sources = library.getSources();
				} catch (DbException e) {
					new MorriganMsgDlg(e);
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
//	Actions.
	
	private IAction addAction = new Action("add", Activator.getImageDescriptor("icons/plus.gif")) {
		public void run() {
			if (library==null) {
				new MorriganMsgDlg("No library selected desu~.").open();
				return;
			}
			
			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Add Source");
			dlg.setMessage("Select a directory containing media.");
			String dir = dlg.open();
			if (dir != null) {
				try {
					library.addSource(dir);
				} catch (DbException e) {
					new MorriganMsgDlg(e).open();
				}
				updateGui();
			}
		};
	};
	
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
					updateGui();
				}
			}
			
			
		};
	};
	
	private IAction updateAction = new Action("update", Activator.getImageDescriptor("icons/search.gif")) {
		public void run() {
			if (library==null) {
				new MorriganMsgDlg("No library selected desu~.").open();
				return;
			}
			
			LibraryUpdateTask job = new LibraryUpdateTask(library);
			job.schedule();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

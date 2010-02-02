package net.sparktank.morrigan.editors;

import java.io.File;

import net.sparktank.morrigan.ApplicationActionBarAdvisor;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaTrack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

public class MediaListEditor extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.editors.MediaListEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaList editedMediaList;
	private TableViewer editTable;
	
	private boolean m_isDirty = false;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public MediaListEditor() {
		super();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	EditorPart methods.
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		editedMediaList = ((MediaListEditorInput) input).getEditedMediaList();
		setPartName(editedMediaList.getListName());
	}
	
	@Override
	public void createPartControl(Composite parent) {
		// Create table control.
		editTable = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		// add and configure columns.
		String[] titles = { "file", "size" };
		int[] bounds = { 400, 100 };
		
		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(editTable, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setWidth(bounds[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
		}
		Table table = editTable.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// where the data and lables are coming from.
		editTable.setContentProvider(contentProvider);
		editTable.setLabelProvider(labelProvider);
		
		// finishing off.
		editTable.setInput(getEditorSite());
	}
	
	@Override
	public void setFocus() {
		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ADDACTIONID, addAction);
	}
	
	@Override
	public boolean isDirty() {
		return m_isDirty;
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	@Override
	public void doSaveAs() {
		new MorriganMsgDlg("TODO: do save as for '" + getTitle() + "'.\n\n(this should not happen.)").open();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO save changes.
		new MorriganMsgDlg("TODO: do save " + getTitle()).open();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Editor helper methods.
	
	private void setIsDirty (boolean dirty) {
		m_isDirty = dirty;
		firePropertyChange(PROP_DIRTY);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Providers.
	
	IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return editedMediaList.getMediaTracks().toArray();
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	ITableLabelProvider labelProvider = new ITableLabelProvider() {
		
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			MediaTrack elm = (MediaTrack) element;
			
			switch (columnIndex) {
				case 0:
					return elm.getFilepath();
				
				case 1:
					return "0";
				
				default:
					throw new IllegalArgumentException("Invalid column; '" + columnIndex + "'.");
				
			}
		}
		
		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {}
		@Override
		public void dispose() {}
		@Override
		public void removeListener(ILabelProviderListener listener) {}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Methods for editing editedMediaList.
	
	private void addTrack (String file) {
		editedMediaList.addTrack(file);
		editTable.refresh();
		setIsDirty(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	IAction addAction = new Action("add") {
		public void run () {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			
			FileDialog dialog = new FileDialog(shell, SWT.MULTI);
			dialog.setText("Add to " + getTitle());
			dialog.setFilterExtensions(new String[] {"*.mp3", "*.*"}); // TODO refine file type list.
			dialog.setFilterNames(new String[] {"mp3 Files", "All Files"});
			
			String firstSel = dialog.open();
			if (firstSel != null) {
				File firstSelFile = new File(firstSel);
				String baseDir = firstSelFile.getAbsoluteFile().getParentFile().getAbsolutePath();
				
				String[] files = dialog.getFileNames();
				for (String file : files) {
					String toAdd = baseDir + File.separatorChar + file;
					System.out.println("add f=" + toAdd);
					addTrack(toAdd);
				}
			}
			
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.editors;

import java.util.ArrayList;

import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaTrack;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

abstract class MediaListEditor<T extends MediaList> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.editors.MediaListEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private T editedMediaList;
	
	private TableViewer editTable;
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public MediaListEditor() {
		super();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	EditorPart methods.
	
	@SuppressWarnings("unchecked") // TODO I wish I knew how to avoid needing this.
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		if (input instanceof MediaListEditorInput<?>) {
			editedMediaList = ((MediaListEditorInput<T>) input).getEditedMediaList();
		} else {
			throw new IllegalArgumentException("input is not instanceof MediaListEditorInput<?>.");
		}
		
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
	
	protected void refreshUi () {
		editTable.refresh();
	}
	
	protected T getEditedMediaList () {
		return editedMediaList;
	}
	
	protected void addTrack (String file) {
		editedMediaList.addTrack(file);
		editTable.refresh();
	}
	
	protected void removeTrack (MediaTrack track) {
		removeTrack(track, true);
	}
	
	protected void removeTrack (MediaTrack track, boolean refresh) {
		editedMediaList.removeMediaTrack(track);
		if (refresh) editTable.refresh();
	}
	
	protected ArrayList<MediaTrack> getSelectedTracks () {
		ISelection selection = editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			ArrayList<MediaTrack> ret = new ArrayList<MediaTrack>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof MediaTrack) {
						MediaTrack track = (MediaTrack) selectedObject;
						ret.add(track);
					}
				}
			}
			return ret;
		}
		
		return null;
	}
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

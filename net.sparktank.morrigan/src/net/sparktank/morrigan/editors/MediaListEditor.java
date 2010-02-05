package net.sparktank.morrigan.editors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.handler.CallPlayMedia;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaTrack;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public abstract class MediaListEditor<T extends MediaList> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.editors.MediaListEditor";
	
	public enum MediaColumn { FILE, DADDED }
	
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
		
		try {
			editedMediaList.read();
		} catch (MorriganException e) {
			throw new PartInitException("Exception while calling read().", e);
		}
		
		setPartName(editedMediaList.getListName());
		
		editedMediaList.addDirtyChangeEvent(dirtyChange);
		editedMediaList.addChangeEvent(listChange);
	}
	
	@Override
	public void dispose() {
		editedMediaList.removeChangeEvent(listChange);
		editedMediaList.removeDirtyChangeEvent(dirtyChange);
		super.dispose();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		// Create table control.
		Composite comp = new Composite(parent, SWT.NONE);
		editTable = new TableViewer(comp, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION );
		TableColumnLayout layout = new TableColumnLayout();
		comp.setLayout(layout);
		
		// add and configure columns.
		MediaColumn[] titles = MediaColumn.values();
		ColumnLayoutData[] bounds = {new ColumnWeightData(100), new ColumnPixelData(140, true, true) };
		
		for (int i = 0; i < titles.length; i++) {
			final TableViewerColumn column = new TableViewerColumn(editTable, SWT.NONE);
			layout.setColumnData(column.getColumn(), bounds[i]);
			column.getColumn().setText(titles[i].toString());
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			
			if (isSortable()) {
				column.getColumn().addSelectionListener(getSelectionAdapter(editTable, column));
			}
		}
		
		Table table = editTable.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		
		// where the data and lables are coming from.
		editTable.setContentProvider(contentProvider);
		editTable.setLabelProvider(labelProvider);
		
		// event handelers.
		editTable.addDoubleClickListener(doubleClickListener);
		
		// finishing off.
		editTable.setInput(getEditorSite());
	}
	
	@Override
	public boolean isDirty() {
		return editedMediaList.isDirty();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Providers.
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return editedMediaList.getMediaTracks().toArray();
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	private ITableLabelProvider labelProvider = new ITableLabelProvider() {
		
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			MediaTrack elm = (MediaTrack) element;
			
			switch (columnIndex) {
				case 0:
					return elm.getTitle();
				
				case 1:
					return elm.getDateAdded() == null ? null : sdf.format(elm.getDateAdded());
					
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
//	Event handelers.
	
	private Runnable dirtyChange = new Runnable() {
		@Override
		public void run() {
			if (!dirtyChangedRunableScheduled) {
				dirtyChangedRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(dirtyChangedRunable);
			}
		}
	};
	
	private Runnable listChange = new Runnable() {
		@Override
		public void run() {
			if (!updateGuiRunableScheduled) {
				updateGuiRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(updateGuiRunable);
			}
		}
	};
	
	private volatile boolean dirtyChangedRunableScheduled = false;
	
	private Runnable dirtyChangedRunable = new Runnable() {
		@Override
		public void run() {
			dirtyChangedRunableScheduled = false;
			firePropertyChange(PROP_DIRTY);
		}
	};
	
	private volatile boolean updateGuiRunableScheduled = false;
	
	private Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			updateGuiRunableScheduled = false;
			if (editTable.getTable().isDisposed()) return;
			editTable.refresh();
		}
	};
	
	private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		@Override
		public void doubleClick(DoubleClickEvent event) {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(CallPlayMedia.ID, null);
			} catch (CommandException e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	abstract protected boolean isSortable ();
	
	abstract protected void onSort (final TableViewer table, final TableViewerColumn column, int direction);
	
	private SelectionAdapter getSelectionAdapter (final TableViewer table, final TableViewerColumn column) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int dir = table.getTable().getSortDirection();
				if (table.getTable().getSortColumn() == column.getColumn()) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					dir = SWT.DOWN;
				}
				onSort(table, column, dir);
				table.getTable().setSortDirection(dir);
				table.getTable().setSortColumn(column.getColumn());
			}
		};
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Methods for editing editedMediaList.
	
	public T getEditedMediaList () {
		return editedMediaList;
	}
	
	protected void addTrack (String file) {
		editedMediaList.addTrack(new MediaTrack(file));
	}
	
	protected void removeTrack (MediaTrack track) {
		editedMediaList.removeMediaTrack(track);
	}
	
	public MediaTrack getSelectedTrack () {
		ISelection selection = editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object selectedObject = iSel.getFirstElement();
			if (selectedObject != null) {
				if (selectedObject instanceof MediaTrack) {
					MediaTrack track = (MediaTrack) selectedObject;
					return track;
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<MediaTrack> getSelectedTracks () {
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

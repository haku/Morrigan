package net.sparktank.morrigan.editors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.handler.CallPlayMedia;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaList.DirtyState;
import net.sparktank.morrigan.preferences.MediaListPref;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public abstract class MediaListEditor<T extends MediaList> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constants and Enums.
	
	public static final String ID = "net.sparktank.morrigan.editors.MediaListEditor";
	
	public enum MediaColumn { 
		FILE       {@Override public String toString() { return "file"; } }, 
		DADDED     {@Override public String toString() { return "added"; } },
		COUNTS     {@Override public String toString() { return "counts"; } },
		DLASTPLAY  {@Override public String toString() { return "last played"; } }
		}
	
	public static MediaColumn parseMediaColumn (String s) {
		for (MediaColumn o : MediaColumn.values()) {
			if (s.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException();
	}
	
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
		final int sep = 3;
		FormData formData;
		
		Composite parent2 = new Composite(parent, SWT.NONE);
		parent2.setLayout(new FormLayout());
		
		// Create toolbar.
		
		/* TODO make toolbar?
		 * TODO add icons.
		 * TODO wire-up.
		 * TODO match enabled state with actions / dirty state.
		 * TODO use toolbar instead?
		 * TODO allow subclasses to control which buttons are shown.
		 */
		
		Composite toolbarComposite = new Composite(parent2, SWT.NONE);
		Button btnSave = new Button(toolbarComposite, SWT.PUSH);
		Label lblStatus = new Label(toolbarComposite, SWT.NONE);
		Button btnAdd = new Button(toolbarComposite, SWT.PUSH);
		Button btnRemove = new Button(toolbarComposite, SWT.PUSH);
		Button btnProperties = new Button(toolbarComposite, SWT.PUSH);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.height = sep + btnSave.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + sep; // FIXME
		toolbarComposite.setLayoutData(formData);
		toolbarComposite.setLayout(new FormLayout());
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(0, sep);
		btnSave.setText("Save");
		btnSave.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(btnSave, sep*2);
		formData.right = new FormAttachment(btnAdd, -sep);
		lblStatus.setLayoutData(formData);
		lblStatus.setText("n items.");
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(btnRemove, -sep);
		btnAdd.setText("Add");
		btnAdd.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(btnProperties, -sep);
		btnRemove.setText("Remove");
		btnRemove.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(100, -sep);
		btnProperties.setText("Properties");
		btnProperties.setLayoutData(formData);
		
		// Create table.
		
		Composite tableComposite = new Composite(parent2, SWT.NONE); // Because of the way table column layouts work.
		formData = new FormData();
		formData.top = new FormAttachment(toolbarComposite, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		tableComposite.setLayoutData(formData);
		editTable = new TableViewer(tableComposite, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION );
		TableColumnLayout layout = new TableColumnLayout();
		tableComposite.setLayout(layout);
		
		// add and configure columns.
		MediaColumn[] titles = MediaColumn.values();
		
		for (int i = 0; i < titles.length; i++) {
			if (MediaListPref.getColPref(titles[i])) {
				final TableViewerColumn column = new TableViewerColumn(editTable, SWT.NONE);
				
				switch (titles[i]) {
					case FILE:
						layout.setColumnData(column.getColumn(), new ColumnWeightData(100));
						column.setLabelProvider(new FileLblProv());
						break;
					
					case DADDED:
						layout.setColumnData(column.getColumn(), new ColumnPixelData(140, true, true));
						column.setLabelProvider(new DateAddedLblProv());
						break;
						
					case COUNTS:
						layout.setColumnData(column.getColumn(), new ColumnPixelData(70, true, true));
						column.setLabelProvider(new CountsLblProv());
						column.getColumn().setAlignment(SWT.CENTER);
						break;
						
					case DLASTPLAY:
						layout.setColumnData(column.getColumn(), new ColumnPixelData(140, true, true));
						column.setLabelProvider(new DateLastPlayerLblProv());
						break;
					
					default:
						throw new IllegalArgumentException();
					
				}
				
				column.getColumn().setText(titles[i].toString());
				column.getColumn().setResizable(true);
				column.getColumn().setMoveable(true);
				
				if (isSortable()) {
					column.getColumn().addSelectionListener(getSelectionAdapter(editTable, column));
				}
			}
		}
		
		Table table = editTable.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		
		editTable.setContentProvider(contentProvider);
		
		editTable.addDoubleClickListener(doubleClickListener);
		editTable.setInput(getEditorSite());
	}
	
	@Override
	public boolean isDirty() {
		return editedMediaList.getDirtyState() == DirtyState.DIRTY;
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
	
	private class FileLblProv extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			return elm.getTitle() == null ? null : elm.getTitle();
		}
	}
	
	private class DateAddedLblProv extends ColumnLabelProvider {
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			return elm.getDateAdded() == null ? null : sdf.format(elm.getDateAdded());
		}
	}
	
	private class CountsLblProv extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			if (elm.getStartCount() <= 0 && elm.getStartCount() <= 0) {
				return null;
			} else {
				return String.valueOf(elm.getStartCount()) + "/" + String.valueOf(elm.getEndCount());
			}
		}
	}
	
	private class DateLastPlayerLblProv extends ColumnLabelProvider {
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			return elm.getDateLastPlayed() == null ? null : sdf.format(elm.getDateLastPlayed());
		}
	}
	
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
		editedMediaList.addTrack(new MediaItem(file));
	}
	
	protected void removeTrack (MediaItem track) {
		editedMediaList.removeMediaTrack(track);
	}
	
	public MediaItem getSelectedTrack () {
		ISelection selection = editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object selectedObject = iSel.getFirstElement();
			if (selectedObject != null) {
				if (selectedObject instanceof MediaItem) {
					MediaItem track = (MediaItem) selectedObject;
					return track;
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<MediaItem> getSelectedTracks () {
		ISelection selection = editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			ArrayList<MediaItem> ret = new ArrayList<MediaItem>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof MediaItem) {
						MediaItem track = (MediaItem) selectedObject;
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

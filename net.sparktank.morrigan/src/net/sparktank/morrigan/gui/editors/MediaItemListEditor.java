package net.sparktank.morrigan.gui.editors;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.adaptors.MediaFilter;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.CallPlayMedia;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.gui.preferences.MediaListPref;
import net.sparktank.morrigan.model.IMediaItemList;
import net.sparktank.morrigan.model.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.tasks.MediaFileCopyTask;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

/*
 * TODO Finish extracting generic stuff from MediaTrackListEditor.
 */
public abstract class MediaItemListEditor<T extends IMediaItemList<S>, S extends MediaItem> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Nested classes.
	
	private MediaItemListEditorInput<T> editorInput;
	
	TableViewer editTable = null;
	protected MediaFilter<T, S> mediaFilter = null;
	private ImageCache imageCache = new ImageCache();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.
	
	protected abstract S getNewS (String filePath);
	
	protected abstract List<MediaColumn> getColumns ();
	protected abstract boolean isColumnVisible (MediaColumn col);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	EditorPart methods.
	
	@SuppressWarnings("unchecked") // TODO I wish I knew how to avoid needing this.
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		if (input instanceof MediaItemListEditorInput<?>) {
			this.editorInput = (MediaItemListEditorInput<T>) input;
		} else {
			throw new IllegalArgumentException("input is not instanceof MediaItemListEditorInput<?>.");
		}
		
		setPartName(this.editorInput.getMediaList().getListName());
		
		this.editorInput.getMediaList().addDirtyChangeEvent(this.dirtyChange);
		this.editorInput.getMediaList().addChangeEvent(this.listChange);
		
		try {
			readInputData();
		} catch (Exception e) {
			if (!handleReadError(e)) {
				throw new PartInitException("Exception while calling readInputData().", e);
			}
		}
	}
	
	@Override
	public void dispose() {
		this.editorInput.getMediaList().removeChangeEvent(this.listChange);
		this.editorInput.getMediaList().removeDirtyChangeEvent(this.dirtyChange);
		this.imageCache.clearCache();
		super.dispose();
	}
	
	protected void readInputData () throws MorriganException {
		this.editorInput.getMediaList().read();
	}
	
	protected abstract boolean handleReadError (Exception e);
	
	@Override
	public MediaItemListEditorInput<T> getEditorInput() {
		return this.editorInput;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Controls.
	
	protected final int sep = 3;
	
	abstract protected void createControls (Composite parent);
	abstract protected List<Control> populateToolbar (Composite parent);
	abstract protected void populateContextMenu (List<IContributionItem> menu0, List<IContributionItem> menu1);
	
	protected Label lblStatus = null;
	
	@Override
	public void createPartControl(Composite parent) {
		FormData formData;
		
		Composite parent2 = new Composite(parent, SWT.NONE);
		parent2.setLayout(new FormLayout());
		
		// Create toolbar area.
		
		Composite toolbarComposite = new Composite(parent2, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		toolbarComposite.setLayoutData(formData);
		toolbarComposite.setLayout(new FormLayout());
		
		// Create table.
		
		Composite tableComposite = new Composite(parent2, SWT.NONE); // Because of the way table column layouts work.
		formData = new FormData();
		formData.top = new FormAttachment(toolbarComposite, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		tableComposite.setLayoutData(formData);
		this.editTable = new TableViewer(tableComposite, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION );
		TableColumnLayout layout = new TableColumnLayout();
		tableComposite.setLayout(layout);
		
		// add and configure columns.
		for (MediaColumn mCol : getColumns()) {
			if (isColumnVisible(mCol)) {
				final TableViewerColumn column = new TableViewerColumn(this.editTable, SWT.NONE);
				
				layout.setColumnData(column.getColumn(), mCol.getColumnLayoutData());
				column.setLabelProvider(mCol.getCellLabelProvider());
				if (mCol.getAlignment() > -1) {
					column.getColumn().setAlignment(mCol.getAlignment());
				}
				
				column.getColumn().setText(mCol.toString());
				column.getColumn().setResizable(true);
				column.getColumn().setMoveable(true);
				
				if (isSortable()) {
					column.getColumn().addSelectionListener(getSelectionAdapter(this.editTable, column));
				}
			}
		}
		
		Table table = this.editTable.getTable();
		table.setHeaderVisible(MediaListPref.getShowHeadersPref());
		table.setLinesVisible(false);
		
		this.editTable.setContentProvider(this.contentProvider);
		this.editTable.addDoubleClickListener(this.doubleClickListener);
		this.editTable.setInput(getEditorSite());
		this.mediaFilter = new MediaFilter<T, S>();
		this.editTable.addFilter(this.mediaFilter);
		
		getSite().setSelectionProvider(this.editTable);
		
		int topIndex = this.editorInput.getTopIndex();
		if (topIndex > 0) {
			this.editTable.getTable().setTopIndex(topIndex);
		}
		this.editorInput.setTable(this.editTable.getTable());
		
		createControls(parent2);
		createToolbar(toolbarComposite);
		createContextMenu(parent2);
		
		// Call update events.
		listChanged();
	}
	
	@Override
	public boolean isDirty() {
		return this.editorInput.getMediaList().getDirtyState() == DirtyState.DIRTY;
	}
	
	private void createToolbar (Composite toolbarParent) {
		FormData formData;
		
		List<Control> controls = populateToolbar(toolbarParent);
		
		this.lblStatus = new Label(toolbarParent, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(50, -(this.lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(0, this.sep*2);
		formData.right = new FormAttachment(controls.get(0), -this.sep);
		this.lblStatus.setLayoutData(formData);
		
		for (int i = 0; i < controls.size(); i++) {
			formData = new FormData();
			
			formData.top = new FormAttachment(0, this.sep);
			formData.bottom = new FormAttachment(100, -this.sep);
			
			if (i == controls.size() - 1) {
				formData.right = new FormAttachment(100, -this.sep);
			} else {
				formData.right = new FormAttachment(controls.get(i+1), -this.sep);
			}
			
			controls.get(i).setLayoutData(formData);
		}
	}
	
	private void createContextMenu (Composite parent) {
		List<IContributionItem> menu0 = new LinkedList<IContributionItem>();
		List<IContributionItem> menu1 = new LinkedList<IContributionItem>();
		populateContextMenu(menu0, menu1);
		
		MenuManager contextMenuMgr = new MenuManager();
		for (IContributionItem a : menu0) {
			contextMenuMgr.add(a);
		}
		contextMenuMgr.add(new Separator());
		for (IContributionItem a : menu1) {
			contextMenuMgr.add(a);
		}
		setTableMenu(contextMenuMgr.createContextMenu(parent));
	}
	
	protected void setTableMenu (Menu menu) {
		this.editTable.getTable().setMenu(menu);
	}
	
	public void revealItem (Object element) {
		this.editTable.setSelection(new StructuredSelection(element), true);
		this.editTable.getTable().setFocus();
	}
	
	protected void setFilterString (String s) {
		this.mediaFilter.setFilterString(s);
		this.editTable.refresh();
	}
	
	protected ImageCache getImageCache() {
		return this.imageCache;
	}
	
	@Override
	public void setFocus() {
		this.editTable.getTable().setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Providers.
	
	protected IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return MediaItemListEditor.this.getEditorInput().getMediaList().getMediaTracks().toArray();
		}
		
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
		
	};
	
	/**
	 * This will be called on the GUI thread.
	 */
	abstract protected void listChanged ();
	
	private Runnable dirtyChange = new Runnable() {
		@Override
		public void run() {
			if (!MediaItemListEditor.this.dirtyChangedRunableScheduled) {
				MediaItemListEditor.this.dirtyChangedRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(MediaItemListEditor.this.dirtyChangedRunable);
			}
		}
	};
	
	private Runnable listChange = new Runnable() {
		@Override
		public void run() {
			if (!MediaItemListEditor.this.updateGuiRunableScheduled) {
				MediaItemListEditor.this.updateGuiRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(MediaItemListEditor.this.updateGuiRunable);
			}
		}
	};
	
	volatile boolean dirtyChangedRunableScheduled = false;
	
	Runnable dirtyChangedRunable = new Runnable() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			MediaItemListEditor.this.dirtyChangedRunableScheduled = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	};
	
	volatile boolean updateGuiRunableScheduled = false;
	
	Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			MediaItemListEditor.this.updateGuiRunableScheduled = false;
			if (MediaItemListEditor.this.editTable.getTable().isDisposed()) return;
			MediaItemListEditor.this.editTable.refresh();
			listChanged();
		}
	};
	
	protected IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
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
//	Column related methods.
	
	protected MediaColumn parseMediaColumn (String humanName) {
		for (MediaColumn o : getColumns()) {
			if (humanName.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	abstract protected boolean isSortable ();
	
	abstract protected void onSort (final TableViewer table, final TableViewerColumn column, int direction);
	
	protected SelectionAdapter getSelectionAdapter (final TableViewer table, final TableViewerColumn column) {
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
	
	protected void setSortMarker (TableViewerColumn column, int swtDirection) {
		this.editTable.getTable().setSortDirection(swtDirection);
		if (column != null) {
			this.editTable.getTable().setSortColumn(column.getColumn());
		} else {
			this.editTable.getTable().setSortColumn(null);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Methods for editing editedMediaList.
	
	public T getMediaList () {
		return this.editorInput.getMediaList();
	}
	
	public void addItem (String file) {
		this.editorInput.getMediaList().addTrack(getNewS(file));
	}
	
	protected void removeItem (S track) throws MorriganException {
		this.editorInput.getMediaList().removeMediaTrack(track);
	}
	
	public S getSelectedItem () {
		ISelection selection = this.editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object selectedObject = iSel.getFirstElement();
			if (selectedObject != null) {
				if (selectedObject instanceof MediaItem) {
					@SuppressWarnings("unchecked") // FIXME is there a way to avoid needing this?
					S track = (S) selectedObject;
					return track;
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<S> getSelectedItems () {
		ISelection selection = this.editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			ArrayList<S> ret = new ArrayList<S>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof MediaItem) {
						@SuppressWarnings("unchecked") // FIXME is there a way to avoid needing this?
						S track = (S) selectedObject;
						ret.add(track);
					}
				}
			}
			return ret;
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Menus and Actions.
	
	protected IAction removeAction = new Action("Remove") {
		@Override
		public void run () {
			MorriganMsgDlg dlg = new MorriganMsgDlg("Remove selected from " + getTitle() + "?", MorriganMsgDlg.YESNO);
			dlg.open();
			if (dlg.getReturnCode() == Window.OK) {
				for (S track : getSelectedItems()) {
					try {
						removeItem(track);
					} catch (Throwable t) {
						// TODO something more useful here.
						t.printStackTrace();
					}
				}
			}
		}
	};
	
	String lastFileCopyTargetDir = null;
	
	protected IAction copyToAction = new Action("Copy to...") {
		@Override
		public void run () {
			ArrayList<S> selectedTracks = getSelectedItems();
			
			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Copy Files...");
			dlg.setMessage("Select a directory to copy files to.");
			if (MediaItemListEditor.this.lastFileCopyTargetDir != null) {
				dlg.setFilterPath(MediaItemListEditor.this.lastFileCopyTargetDir);
			}
			String dir = dlg.open();
			
			if (dir != null) {
				MediaItemListEditor.this.lastFileCopyTargetDir = dir;
				
				MediaFileCopyTask<S> task = new MediaFileCopyTask<S>(getMediaList(), selectedTracks, new File(dir));
				TaskJob job = new TaskJob(task, getSite().getShell().getDisplay());
				job.schedule();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

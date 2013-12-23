package com.vaguehope.morrigan.gui.editors;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.adaptors.MediaFilter;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.handler.CallPlayMedia;
import com.vaguehope.morrigan.gui.helpers.ClipboardHelper;
import com.vaguehope.morrigan.gui.helpers.ImageCache;
import com.vaguehope.morrigan.gui.helpers.RefreshTimer;
import com.vaguehope.morrigan.gui.jobs.TaskJob;
import com.vaguehope.morrigan.gui.preferences.MediaListPref;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.MediaItemListChangeListener;
import com.vaguehope.morrigan.tasks.MorriganTask;

/*
 * TODO Finish extracting generic stuff from MediaTrackListEditor.
 */
public abstract class MediaItemListEditor<T extends IMediaItemList<S>, S extends IMediaItem> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Nested classes.

	private MediaItemListEditorInput<T> editorInput;

	TableViewer editTable = null;
	Composite editTableComposite = null;
	TableColumnLayout editTableCompositeTableColumnLayout = null;

	protected MediaFilter mediaFilter = null;
	private final ImageCache imageCache = new ImageCache();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.

	protected abstract List<MediaColumn> getColumns ();

	protected abstract boolean isColumnVisible (MediaColumn col);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	EditorPart methods.

	@SuppressWarnings("unchecked") // TODO I wish I knew how to avoid needing this.
	@Override
	public void init (final IEditorSite site, final IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		if (input instanceof MediaItemListEditorInput<?>) {
			this.editorInput = (MediaItemListEditorInput<T>) input;
		}
		else {
			throw new IllegalArgumentException("input is not instanceof MediaItemListEditorInput<?>.");
		}

		setPartName(this.editorInput.getMediaList().getListName());

		makeRefreshers();
		this.editorInput.getMediaList().addChangeEventListener(this.listChangeListener);

		try {
			readInputData();
		}
		catch (Exception e) {
			if (!handleReadError(e)) {
				throw new PartInitException("Exception while calling readInputData().", e);
			}
		}
	}

	@Override
	public void dispose () {
		removePropListener();
		this.editorInput.getMediaList().removeChangeEventListener(this.listChangeListener);
		this.imageCache.clearCache();
		super.dispose();
	}

	protected void readInputData () throws MorriganException {
		this.editorInput.getMediaList().read();
	}

	protected abstract boolean handleReadError (Exception e);

	@Override
	public MediaItemListEditorInput<T> getEditorInput () {
		return this.editorInput;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Controls.

	protected final int sep = 3;

	protected abstract void createControls (Composite parent);

	protected abstract List<Control> populateToolbar (Composite parent);

	protected abstract void populateContextMenu (List<IContributionItem> menu0, List<IContributionItem> menu1);

	protected Label lblStatus = null;

	@Override
	public void createPartControl (final Composite parent) {
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

		this.editTableComposite = new Composite(parent2, SWT.NONE); // Because of the way table column layouts work.
		formData = new FormData();
		formData.top = new FormAttachment(toolbarComposite, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		this.editTableComposite.setLayoutData(formData);
		this.editTable = new TableViewer(this.editTableComposite, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		this.editTableCompositeTableColumnLayout = new TableColumnLayout();
		this.editTableComposite.setLayout(this.editTableCompositeTableColumnLayout);

		// add and configure columns.
		updateColumns();

		this.editTable.getTable().setLinesVisible(false);

		this.editTable.setContentProvider(this.contentProvider);
		this.editTable.addDoubleClickListener(this.doubleClickListener);
		this.editTable.getTable().addMouseListener(this.middleClickListener);
		this.editTable.setInput(getEditorSite());

		this.mediaFilter = new MediaFilter();
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

		initPropListener();
	}

	protected void updateColumns () {
		updateColumns(getColumns());
	}

	protected void updateColumns (final List<MediaColumn> mediaColumns) {
		TableColumn[] tableColumns = this.editTable.getTable().getColumns();

		for (TableColumn tableColumn : tableColumns) {
			boolean b = false;
			for (MediaColumn mCol : mediaColumns) {
				if (tableColumn.getText().equals(mCol.toString())) {
					b = true;
					break;
				}
			}
			if (b) continue;

			tableColumn.dispose();
		}

		tableColumns = this.editTable.getTable().getColumns(); // Since we just changed this list.
		for (MediaColumn mCol : mediaColumns) {
			boolean b = false;
			for (TableColumn tableColumn : tableColumns) {
				if (tableColumn.getText().equals(mCol.toString())) {
					b = true;
					break;
				}
			}
			if (b) continue;

			if (isColumnVisible(mCol)) {
				final TableViewerColumn column = new TableViewerColumn(this.editTable, SWT.NONE);

				this.editTableCompositeTableColumnLayout.setColumnData(column.getColumn(), mCol.getColumnLayoutData());
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

		this.editTable.getTable().setHeaderVisible(MediaListPref.getShowHeadersPref());
	}

	protected void refreshColumns () {
		this.editTable.getTable().getParent().layout();
	}

	@Override
	public boolean isDirty () {
		return this.editorInput.getMediaList().getDirtyState() == DirtyState.DIRTY;
	}

	private void createToolbar (final Composite toolbarParent) {
		FormData formData;

		List<Control> controls = populateToolbar(toolbarParent);

		this.lblStatus = new Label(toolbarParent, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(50, -(this.lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y) / 2);
		formData.left = new FormAttachment(0, this.sep * 2);
		formData.right = new FormAttachment(controls.get(0), -this.sep);
		this.lblStatus.setLayoutData(formData);

		for (int i = 0; i < controls.size(); i++) {
			formData = new FormData();

			formData.top = new FormAttachment(0, this.sep);
			formData.bottom = new FormAttachment(100, -this.sep);

			if (i == controls.size() - 1) {
				formData.right = new FormAttachment(100, -this.sep);
			}
			else {
				formData.right = new FormAttachment(controls.get(i + 1), -this.sep);
			}

			controls.get(i).setLayoutData(formData);
		}
	}

	private void createContextMenu (final Composite parent) {
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

	protected void setTableMenu (final Menu menu) {
		this.editTable.getTable().setMenu(menu);
	}

	public void revealItem (final Object element) {
		revealItem(element, true);
	}

	public void revealItem (final Object element, final boolean setFocus) {
		this.editTable.setSelection(new StructuredSelection(element), true);
		if (setFocus) this.editTable.getTable().setFocus();
	}

	protected void setFilterString (final String s) {
		this.mediaFilter.setFilterString(s);
		this.editTable.refresh();
	}

	protected ImageCache getImageCache () {
		return this.imageCache;
	}

	@Override
	public void setFocus () {
		this.editTable.getTable().setFocus();
	}

	private void initPropListener () {
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(this.propListener);
	}

	private void removePropListener () {
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(this.propListener);
	}

	private final IPropertyChangeListener propListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange (final PropertyChangeEvent event) {
			System.err.println("TODO: propertyChange=" + event.getProperty());
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Providers.

	protected IStructuredContentProvider contentProvider = new IStructuredContentProvider() {

		@Override
		public Object[] getElements (final Object inputElement) {
			return MediaItemListEditor.this.getEditorInput().getMediaList().getMediaItems().toArray();
		}

		@Override
		public void dispose () {/* UNUSED */}

		@Override
		public void inputChanged (final Viewer viewer, final Object oldInput, final Object newInput) {/* UNUSED */}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Refreshing.

	private final MediaItemListChangeListener listChangeListener = new MediaItemListChangeListener() {

		@Override
		public void eventMessage (final String msg) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run () {
					getEditorSite().getActionBars().getStatusLineManager().setMessage(msg);
				}
			});
		}

		@Override
		public void dirtyStateChanged (final DirtyState oldState, final DirtyState newState) {
			MediaItemListEditor.this.listDirtyRrefresher.run();
		}

		@Override
		public void mediaListRead () {
			MediaItemListEditor.this.listChangeRrefresher.run();
		}

		@Override
		public void mediaItemsAdded (final IMediaItem... items) {
			updateListItems(items);
		}

		@Override
		public void mediaItemsRemoved (final IMediaItem... items) {
			updateListItems(items);
		}

		@Override
		public void mediaItemsUpdated (final IMediaItem... items) {
			updateListItems(items);
		}

		private void updateListItems (final IMediaItem... items) {
			if (items != null) {
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run () {
						MediaItemListEditor.this.editTable.update(items, null);
					}
				});
			}
			else {
				System.err.println("Warning: full UI list refresh unnecessarily requested.");
				MediaItemListEditor.this.listChangeRrefresher.run();
			}
		}

		@Override
		public void mediaItemsForceReadRequired (final IMediaItem... items) {
			MediaItemListEditor.this.listRequeryRefresher.run();
		}

	};

	protected Runnable listChangeRrefresher;
	protected Runnable listDirtyRrefresher;
	protected Runnable listRequeryRefresher;

	private void makeRefreshers () {
		this.listChangeRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@Override
			public void run () {
				if (MediaItemListEditor.this.editTable.getTable().isDisposed()) return;
				MediaItemListEditor.this.editTable.refresh();
				listChanged();
			}
		});

		this.listDirtyRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run () {
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});

		this.listRequeryRefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@Override
			public void run () {
				try {
					getMediaList().forceRead();
				}
				catch (MorriganException e) {
					MediaItemListEditor.this.logger.log(Level.WARNING, "Exception during requery event.", e);
				}
			}
		});
	}

	/**
	 * This will be called on the GUI thread.
	 */
	protected abstract void listChanged ();

	protected IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		@Override
		public void doubleClick (final DoubleClickEvent event) {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(CallPlayMedia.ID, null);
			}
			catch (CommandException e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};

	/**
	 * This will be called on the GUI thread.
	 */
	protected abstract void middleClickEvent (MouseEvent e);

	protected MouseListener middleClickListener = new MouseListener() {
		@Override
		public void mouseUp (final MouseEvent e) {
			if (e.button == 2) { // 1 is left, 2 is middle, 3 is right.
				middleClickEvent(e);
			}
		}
		@Override public void mouseDown (final MouseEvent e) { /* Unused. */ }
		@Override public void mouseDoubleClick (final MouseEvent e) { /* Unused. */ }
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Column related methods.

	protected MediaColumn parseMediaColumn (final String humanName) {
		for (MediaColumn o : getColumns()) {
			if (humanName.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.

	protected abstract boolean isSortable ();

	protected abstract void onSort (final TableViewer table, final TableViewerColumn column, int direction);

	protected SelectionAdapter getSelectionAdapter (final TableViewer table, final TableViewerColumn column) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected (final SelectionEvent e) {
				int dir = table.getTable().getSortDirection();
				if (table.getTable().getSortColumn() == column.getColumn()) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				}
				else {
					dir = SWT.DOWN;
				}
				onSort(table, column, dir);
				table.getTable().setSortDirection(dir);
				table.getTable().setSortColumn(column.getColumn());
			}
		};
	}

	protected void setSortMarker (final TableViewerColumn column, final int swtDirection) {
		this.editTable.getTable().setSortDirection(swtDirection);
		if (column != null) {
			this.editTable.getTable().setSortColumn(column.getColumn());
		}
		else {
			this.editTable.getTable().setSortColumn(null);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Methods for editing editedMediaList.

	public T getMediaList () {
		return this.editorInput.getMediaList();
	}

	protected void removeItem (final S track) throws MorriganException {
		this.editorInput.getMediaList().removeItem(track);
	}

	public S getSelectedItem () {
		ISelection selection = this.editTable.getSelection();

		if (selection == null || selection.isEmpty()) return null;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object selectedObject = iSel.getFirstElement();
			if (selectedObject != null) {
				if (selectedObject instanceof IMediaItem) {
					@SuppressWarnings("unchecked") // FIXME is there a way to avoid needing this?
					S track = (S) selectedObject;
					return track;
				}
			}
		}

		return null;
	}

	public List<S> getSelectedItems () {
		ISelection selection = this.editTable.getSelection();

		if (selection == null || selection.isEmpty()) return null;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			ArrayList<S> ret = new ArrayList<S>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof IMediaItem) {
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
					}
					catch (MorriganException e) {
						new MorriganMsgDlg(e).open();
					}
				}
			}
		}
	};

	protected IAction toggleEnabledAction = new Action("Toggle enabled") {
		@Override
		public void run () {
			for (S track : getSelectedItems()) {
				try {
					getEditorInput().getMediaList().setItemEnabled(track, !track.isEnabled());
				}
				catch (MorriganException e) {
					new MorriganMsgDlg(e).open();
				}
			}
		}
	};

	String lastFileCopyTargetDir = null;

	protected IAction copyToAction = new Action("Copy to...") {
		@Override
		public void run () {
			List<S> selectedTracks = getSelectedItems();

			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Copy Files...");
			dlg.setMessage("Select a directory to copy files to.");
			if (MediaItemListEditor.this.lastFileCopyTargetDir != null) {
				dlg.setFilterPath(MediaItemListEditor.this.lastFileCopyTargetDir);
			}
			String dir = dlg.open();

			if (dir != null) {
				MediaItemListEditor.this.lastFileCopyTargetDir = dir;

				MorriganTask task = Activator.getMediaFactory().getMediaFileCopyTask(getMediaList(), selectedTracks, new File(dir));
				TaskJob job = new TaskJob(task);
				job.schedule();
			}
		}
	};

	protected IAction copyFilePath = new Action("Copy paths") {
		@Override
		public void run () {
			String newLine = System.getProperty("line.separator");
			StringBuffer sb = new StringBuffer();
			List<S> items = getSelectedItems();
			for (S item : items) {
				sb.append(item.getFilepath());
				if (items.size() > 1) sb.append(newLine);
			}
			ClipboardHelper.setText(sb.toString());
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

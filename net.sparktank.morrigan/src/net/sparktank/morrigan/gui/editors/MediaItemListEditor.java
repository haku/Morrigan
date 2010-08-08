package net.sparktank.morrigan.gui.editors;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.adaptors.MediaFilter;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.CallPlayMedia;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.IMediaItemList;
import net.sparktank.morrigan.model.MediaItem;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
	
	static public class MediaColumn {
		
		private final String humanName;

		public MediaColumn (String humanName) {
			this.humanName = humanName;
		}
		
		@Override
		public String toString() {
			return this.humanName;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Member variables.
	
	MediaItemListEditorInput<T> editorInput;
	
	TableViewer editTable = null;
	protected MediaFilter<T, S> mediaFilter = null;
	
	ImageCache imageCache = new ImageCache();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Abstract methods.
	
	protected abstract S getNewS (String filePath);
	
	protected abstract List<MediaColumn> getColumns ();
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Providers.
	
	protected IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return MediaItemListEditor.this.editorInput.getMediaList().getMediaTracks().toArray();
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.gui.editors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.AddToQueue;
import net.sparktank.morrigan.gui.handler.CallPlayMedia;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.gui.preferences.MediaListPref;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList;
import net.sparktank.morrigan.model.MediaList.DirtyState;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public abstract class MediaListEditor<T extends MediaList<S>, S extends MediaItem> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constants and Enums.
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.MediaListEditor";
	
	public enum MediaColumn { 
		FILE       {@Override public String toString() { return "file";        } }, 
		COUNTS     {@Override public String toString() { return "counts";      } },
		DADDED     {@Override public String toString() { return "added";       } },
		DLASTPLAY  {@Override public String toString() { return "last played"; } },
		HASHCODE   {@Override public String toString() { return "hash";        } },
		DMODIFIED  {@Override public String toString() { return "modified";    } },
		DURATION   {@Override public String toString() { return "duration";    } }
		}
	
	public static MediaColumn parseMediaColumn (String s) {
		for (MediaColumn o : MediaColumn.values()) {
			if (s.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MediaListEditorInput<T> editorInput;
	
	private TableViewer editTable = null;
	private MediaFilter mediaFilter = null;
	
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
			editorInput = (MediaListEditorInput<T>) input;
		} else {
			throw new IllegalArgumentException("input is not instanceof MediaListEditorInput<?>.");
		}
		
		setPartName(editorInput.getMediaList().getListName());
		
		editorInput.getMediaList().addDirtyChangeEvent(dirtyChange);
		editorInput.getMediaList().addChangeEvent(listChange);
		
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
		editorInput.getMediaList().removeChangeEvent(listChange);
		editorInput.getMediaList().removeDirtyChangeEvent(dirtyChange);
		imageCache.clearCache();
		super.dispose();
	}
	
	protected void readInputData () throws MorriganException {
		editorInput.getMediaList().read();
	}
	
	protected abstract boolean handleReadError (Exception e);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Controls.
	
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
						
					case HASHCODE:
						layout.setColumnData(column.getColumn(), new ColumnPixelData(90, true, true));
						column.setLabelProvider(new HashcodeLblProv());
						column.getColumn().setAlignment(SWT.CENTER);
						break;
						
					case DMODIFIED:
						layout.setColumnData(column.getColumn(), new ColumnPixelData(140, true, true));
						column.setLabelProvider(new DateLastModifiedLblProv());
						break;
						
					case DURATION:
						layout.setColumnData(column.getColumn(), new ColumnPixelData(60, true, true));
						column.setLabelProvider(new DurationLblProv());
						column.getColumn().setAlignment(SWT.RIGHT);
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
		table.setHeaderVisible(MediaListPref.getShowHeadersPref());
		table.setLinesVisible(false);
		
		editTable.setContentProvider(contentProvider);
		editTable.addDoubleClickListener(doubleClickListener);
		editTable.setInput(getEditorSite());
		mediaFilter = new MediaFilter();
		editTable.addFilter(mediaFilter);
		
		int topIndex = editorInput.getTopIndex();
		if (topIndex > 0) {
			editTable.getTable().setTopIndex(topIndex);
		}
		editorInput.setTable(editTable.getTable());
		
		// Populate toolbar.
		populateToolbar(toolbarComposite);
		
		// Call update events.
		listChanged();
	}
	
	@Override
	public boolean isDirty() {
		return editorInput.getMediaList().getDirtyState() == DirtyState.DIRTY;
	}
	
	abstract protected void populateToolbar (Composite parent);
	
	private ImageCache imageCache = new ImageCache();
	
	protected void setTableMenu (Menu menu) {
		editTable.getTable().setMenu(menu);
	}
	
	public void revealTrack (Object element) {
		editTable.setSelection(new StructuredSelection(element), true);
		editTable.getTable().setFocus();
	}
	
	protected void setFilterString (String s) {
		mediaFilter.setFilterString(s);
		editTable.refresh();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Providers.
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return editorInput.getMediaList().getMediaTracks().toArray();
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	private Styler strikeoutItemStyle = new Styler() {
		public void applyStyles(TextStyle textStyle) {
			textStyle.strikeout = true;
		}
	};
	
	private static final String MSG_DEC_MISSING = " (missing)";
	private static final String MSG_DEC_DISABLED = " (disabled)";
	
	private class FileLblProv extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (element instanceof MediaItem) {
				MediaItem mi = (MediaItem) element;
				
				if (mi.getTitle() != null) {
					Styler styler = null;
					if (mi.isMissing() || !mi.isEnabled()) {
						styler = strikeoutItemStyle;
					}
					StyledString styledString = new StyledString(mi.getTitle(), styler);
					
					String dec = null;
					if (mi.isMissing()) {
						dec = MSG_DEC_MISSING;
					} else if (!mi.isEnabled()) {
						dec = MSG_DEC_DISABLED;
					}
					
					if (dec != null) {
						styledString.append(dec, StyledString.DECORATIONS_STYLER);
					}
					
					cell.setText(styledString.toString());
					cell.setStyleRanges(styledString.getStyleRanges());
					
				} else {
					cell.setText(null);
				}
				
				if (mi.isMissing()) {
					cell.setImage(null); // TODO find icon for missing?
				} else if (!mi.isEnabled()) {
					cell.setImage(null); // TODO find icon for disabled?
				} else {
					cell.setImage(imageCache.readImage("icons/playlist.gif")); // TODO find icon for items?
				}
				
			}
			super.update(cell);
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
	
	private class HashcodeLblProv extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			if (elm.getHashcode() == 0) {
				return null;
			} else {
				return Long.toHexString(elm.getHashcode());
			}
		}
	}
	
	private class DateLastModifiedLblProv extends ColumnLabelProvider {
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			return elm.getDateLastModified() == null ? null : sdf.format(elm.getDateLastModified());
		}
	}
	
	private class DurationLblProv extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			if (elm.getDuration() <= 0) {
				return null;
			} else {
				return TimeHelper.formatTimeSeconds(elm.getDuration());
			}
		}
	}
	
	public class MediaFilter extends ViewerFilter {
		
		private String searchTerm;

		public void setFilterString (String s) {
			this.searchTerm = ".*(?i)" + s + ".*";
			
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchTerm == null || searchTerm.length() == 0) {
				return true;
			}
			MediaItem mi = (MediaItem) element;
			if (mi.getFilepath().matches(searchTerm)) {
				return true;
			}
			return false;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Event handelers.
	
	/**
	 * This will be called on the GUI thread.
	 */
	abstract protected void listChanged ();
	
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
			firePropertyChange(EditorPart.PROP_DIRTY);
		}
	};
	
	private volatile boolean updateGuiRunableScheduled = false;
	
	private Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			updateGuiRunableScheduled = false;
			if (editTable.getTable().isDisposed()) return;
			editTable.refresh();
			listChanged();
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
	
	protected void setSortMarker (TableViewerColumn column, int swtDirection) {
		editTable.getTable().setSortDirection(swtDirection);
		if (column != null) {
			editTable.getTable().setSortColumn(column.getColumn());
		} else {
			editTable.getTable().setSortColumn(null);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Methods for editing editedMediaList.
	
	public T getMediaList () {
		return editorInput.getMediaList();
	}
	
	@SuppressWarnings("unchecked")
	protected void addTrack (String file) {
		MediaItem mediaItem = new MediaItem(file);
		editorInput.getMediaList().addTrack((S) mediaItem);
	}
	
	protected void removeTrack (MediaItem track) throws MorriganException {
		editorInput.getMediaList().removeMediaTrack(track);
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
//	Menus and Actions.
	
	protected MenuManager getAddToMenu () {
		final MenuManager menu = new MenuManager("Add to playlist...");
		
		menu.addMenuListener(new IMenuListener () {
			public void menuAboutToShow(IMenuManager manager) {
				IEditorReference[] editors = getEditorSite().getPage().getEditorReferences();
				for (final IEditorReference e : editors) {
					if (e.getId().equals(PlaylistEditor.ID)) {
						menu.add(new AddToPlaylistAction(e));
					}
				}
				if (menu.getItems().length < 1) {
					Action a = new Action("(No playlists open)") {};
					a.setEnabled(false);
					menu.add(a);
				}
			}
		});
		
		menu.setRemoveAllWhenShown(true);
		
		return menu;
	}
	
	protected class AddToPlaylistAction extends Action {
		
		private final IEditorReference editor;
		
		public AddToPlaylistAction (IEditorReference editor) {
			super(editor.getName(), Activator.getImageDescriptor("icons/playlist.gif"));
			editor.getTitleImage();
			this.editor = editor;
		}
		
		@Override
		public void run() {
			super.run();
			IWorkbenchPart part = editor.getPart(false);
			if (part != null && part instanceof PlaylistEditor) {
				PlaylistEditor plPart = (PlaylistEditor) part;
				for (MediaItem track : getSelectedTracks()) {
					plPart.addTrack(track.getFilepath());
				}
			}
		}
		
	}
	
	protected IAction addToQueueAction = new Action("Enqueue") {
		@Override
		public void run() {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(AddToQueue.ID, null);
			} catch (Throwable t) {
				new MorriganMsgDlg(t).open();
			}
		};
	};
	
	protected IAction removeAction = new Action("Remove") {
		@Override
		public void run () {
			MorriganMsgDlg dlg = new MorriganMsgDlg("Remove selected from " + getTitle() + "?", MorriganMsgDlg.YESNO);
			dlg.open();
			if (dlg.getReturnCode() == MorriganMsgDlg.OK) {
				for (MediaItem track : getSelectedTracks()) {
					try {
						removeTrack(track);
					} catch (Throwable t) {
						// TODO something more useful here.
						t.printStackTrace();
					}
				}
			}
		}
	};
	
	protected IAction toggleEnabledAction = new Action("Toggle enabled") {
		@Override
		public void run() {
			for (MediaItem track : getSelectedTracks()) {
				try {
					editorInput.getMediaList().setTrackEnabled(track, !track.isEnabled());
				} catch (Throwable t) {
					// TODO something more useful here.
					t.printStackTrace();
				}
			}
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

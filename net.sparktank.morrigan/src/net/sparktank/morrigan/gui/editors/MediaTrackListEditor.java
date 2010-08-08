package net.sparktank.morrigan.gui.editors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.AddToQueue;
import net.sparktank.morrigan.gui.handler.CallPlayMedia;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.gui.preferences.MediaListPref;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.tracks.IMediaTrackList;
import net.sparktank.morrigan.model.tracks.MediaTrack;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;

public abstract class MediaTrackListEditor<T extends IMediaTrackList<S>, S extends MediaTrack> extends EditorPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constants and Enums.
	
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
	
	MediaItemListEditorInput<T> editorInput;
	
	TableViewer editTable = null;
	private MediaFilter mediaFilter = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public MediaTrackListEditor() {
		super();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	abstract S getNewS (String filePath);
	
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
			throw new IllegalArgumentException("input is not instanceof MediaListEditorInput<?>.");
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
//	Controls.
	
	protected final int sep = 3;
	
	abstract protected void createControls (Composite parent);
	abstract protected List<Control> populateToolbar (Composite parent);
	abstract protected void populateContextMenu (List<IContributionItem> menu0, List<IContributionItem> menu1);
	
	ImageCache imageCache = new ImageCache();
	
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
		MediaColumn[] titles = MediaColumn.values();
		
		for (int i = 0; i < titles.length; i++) {
			if (MediaListPref.getColPref(titles[i])) {
				final TableViewerColumn column = new TableViewerColumn(this.editTable, SWT.NONE);
				
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
	
	public void revealTrack (Object element) {
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
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return MediaTrackListEditor.this.editorInput.getMediaList().getMediaTracks().toArray();
		}
		
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
		
	};
	
	Styler strikeoutItemStyle = new Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.strikeout = true;
		}
	};
	
	private static final String MSG_DEC_MISSING = " (missing)";
	private static final String MSG_DEC_DISABLED = " (disabled)";
	
	private class FileLblProv extends StyledCellLabelProvider {
		
		public FileLblProv () {/* UNUSED */}
		
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (element instanceof MediaItem) {
				MediaItem mi = (MediaItem) element;
				
				if (mi.getTitle() != null) {
					Styler styler = null;
					if (mi.isMissing() || !mi.isEnabled()) {
						styler = MediaTrackListEditor.this.strikeoutItemStyle;
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
					cell.setImage(MediaTrackListEditor.this.imageCache.readImage("icons/playlist.gif")); // TODO find icon for items?
				}
				
			}
			super.update(cell);
		}
	}
	
	private class DateAddedLblProv extends ColumnLabelProvider {
		
		public DateAddedLblProv () {/* UNUSED */}
		
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			return elm.getDateAdded() == null ? null : this.sdf.format(elm.getDateAdded());
		}
	}
	
	private class CountsLblProv extends ColumnLabelProvider {
		
		public CountsLblProv () {/* UNUSED */}
		
		@Override
		public String getText(Object element) {
			MediaTrack elm = (MediaTrack) element;
			if (elm.getStartCount() <= 0 && elm.getStartCount() <= 0) {
				return null;
			}
			
			return String.valueOf(elm.getStartCount()) + "/" + String.valueOf(elm.getEndCount());
		}
		
	}
	
	private class DateLastPlayerLblProv extends ColumnLabelProvider {
		
		public DateLastPlayerLblProv () {/* UNUSED */}
		
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		@Override
		public String getText(Object element) {
			MediaTrack elm = (MediaTrack) element;
			return elm.getDateLastPlayed() == null ? null : this.sdf.format(elm.getDateLastPlayed());
		}
		
	}
	
	private class HashcodeLblProv extends ColumnLabelProvider {
		
		public HashcodeLblProv () {/* UNUSED */}
		
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			if (elm.getHashcode() == 0) {
				return null;
			}
			
			return Long.toHexString(elm.getHashcode());
		}
		
	}
	
	private class DateLastModifiedLblProv extends ColumnLabelProvider {
		
		public DateLastModifiedLblProv () {/* UNUSED */}
		
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		@Override
		public String getText(Object element) {
			MediaItem elm = (MediaItem) element;
			return elm.getDateLastModified() == null ? null : this.sdf.format(elm.getDateLastModified());
		}
		
	}
	
	private class DurationLblProv extends ColumnLabelProvider {
		
		public DurationLblProv () {/* UNUSED */}
		
		@Override
		public String getText(Object element) {
			MediaTrack elm = (MediaTrack) element;
			if (elm.getDuration() <= 0) {
				return null;
			}
			
			return TimeHelper.formatTimeSeconds(elm.getDuration());
		}
		
	}
	
	public class MediaFilter extends ViewerFilter {
		
		private String searchTerm;

		public void setFilterString (String s) {
			this.searchTerm = ".*(?i)" + s + ".*";
			
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (this.searchTerm == null || this.searchTerm.length() == 0) {
				return true;
			}
			MediaItem mi = (MediaItem) element;
			if (mi.getFilepath().matches(this.searchTerm)) {
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
			if (!MediaTrackListEditor.this.dirtyChangedRunableScheduled) {
				MediaTrackListEditor.this.dirtyChangedRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(MediaTrackListEditor.this.dirtyChangedRunable);
			}
		}
	};
	
	private Runnable listChange = new Runnable() {
		@Override
		public void run() {
			if (!MediaTrackListEditor.this.updateGuiRunableScheduled) {
				MediaTrackListEditor.this.updateGuiRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(MediaTrackListEditor.this.updateGuiRunable);
			}
		}
	};
	
	volatile boolean dirtyChangedRunableScheduled = false;
	
	Runnable dirtyChangedRunable = new Runnable() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			MediaTrackListEditor.this.dirtyChangedRunableScheduled = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	};
	
	volatile boolean updateGuiRunableScheduled = false;
	
	Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			MediaTrackListEditor.this.updateGuiRunableScheduled = false;
			if (MediaTrackListEditor.this.editTable.getTable().isDisposed()) return;
			MediaTrackListEditor.this.editTable.refresh();
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
	
	protected void addTrack (String file) {
		this.editorInput.getMediaList().addTrack(getNewS(file));
	}
	
	protected void removeTrack (S track) throws MorriganException {
		this.editorInput.getMediaList().removeMediaTrack(track);
	}
	
	public S getSelectedTrack () {
		ISelection selection = this.editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object selectedObject = iSel.getFirstElement();
			if (selectedObject != null) {
				if (selectedObject instanceof MediaTrack) {
					@SuppressWarnings("unchecked") // FIXME is there a way to avoid needing this?
					S track = (S) selectedObject;
					return track;
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<S> getSelectedTracks () {
		ISelection selection = this.editTable.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			ArrayList<S> ret = new ArrayList<S>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof MediaTrack) {
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
	
	protected MenuManager getAddToMenu () {
		final MenuManager menu = new MenuManager("Add to playlist...");
		
		menu.addMenuListener(new IMenuListener () {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IEditorReference[] editors = getEditorSite().getPage().getEditorReferences();
				for (final IEditorReference e : editors) {
					if (e.getId().equals(PlaylistEditor.ID)) {
						menu.add(new AddToPlaylistAction(e));
					}
				}
				if (menu.getItems().length < 1) {
					Action a = new Action("(No playlists open)") {/* UNUSED */};
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
			IWorkbenchPart part = this.editor.getPart(false);
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
			if (dlg.getReturnCode() == Window.OK) {
				for (S track : getSelectedTracks()) {
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
			for (S track : getSelectedTracks()) {
				try {
					MediaTrackListEditor.this.editorInput.getMediaList().setTrackEnabled(track, !track.isEnabled());
				} catch (Throwable t) {
					// TODO something more useful here.
					t.printStackTrace();
				}
			}
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

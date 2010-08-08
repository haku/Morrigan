package net.sparktank.morrigan.gui.editors;

import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.gui.actions.AddToPlaylistAction;
import net.sparktank.morrigan.gui.adaptors.CountsLblProv;
import net.sparktank.morrigan.gui.adaptors.DateAddedLblProv;
import net.sparktank.morrigan.gui.adaptors.DateLastModifiedLblProv;
import net.sparktank.morrigan.gui.adaptors.DateLastPlayerLblProv;
import net.sparktank.morrigan.gui.adaptors.DurationLblProv;
import net.sparktank.morrigan.gui.adaptors.FileLblProv;
import net.sparktank.morrigan.gui.adaptors.HashcodeLblProv;
import net.sparktank.morrigan.gui.adaptors.MediaFilter;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.handler.AddToQueue;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.gui.preferences.MediaListPref;
import net.sparktank.morrigan.model.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.tracks.IMediaTrackList;
import net.sparktank.morrigan.model.tracks.MediaTrack;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.handlers.IHandlerService;

public abstract class MediaTrackListEditor<T extends IMediaTrackList<S>, S extends MediaTrack> extends MediaItemListEditor<T, S> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Column definitions.
	
	public static final MediaColumn COL_FILE = new MediaColumn("file");
	public static final MediaColumn COL_COUNTS = new MediaColumn("counts");
	public static final MediaColumn COL_ADDED = new MediaColumn("added");
	public static final MediaColumn COL_LASTPLAYED = new MediaColumn("last played");
	public static final MediaColumn COL_HASH = new MediaColumn("hash");
	public static final MediaColumn COL_MODIFIED = new MediaColumn("modified");
	public static final MediaColumn COL_DURATION = new MediaColumn("duration");
	
	public static final MediaColumn[] COLS = new MediaColumn[] {
		COL_FILE,
		COL_COUNTS,
		COL_ADDED,
		COL_LASTPLAYED,
		COL_HASH,
		COL_MODIFIED,
		COL_DURATION
	};
	
	@Override
	protected List<MediaColumn> getColumns() {
		List<MediaColumn> l = new LinkedList<MediaColumn>();
		for (MediaColumn c : COLS) {
			l.add(c);
		}
		return l;
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
		for (MediaColumn mCol : COLS) {
			if (MediaListPref.getColPref(mCol)) {
				final TableViewerColumn column = new TableViewerColumn(this.editTable, SWT.NONE);
				
				if (mCol == COL_FILE) {
					layout.setColumnData(column.getColumn(), new ColumnWeightData(100));
					column.setLabelProvider(new FileLblProv(this.imageCache));
				}
				else if (mCol == COL_ADDED) {
					layout.setColumnData(column.getColumn(), new ColumnPixelData(140, true, true));
					column.setLabelProvider(new DateAddedLblProv());
				}
				else if (mCol == COL_COUNTS) {
					layout.setColumnData(column.getColumn(), new ColumnPixelData(70, true, true));
					column.setLabelProvider(new CountsLblProv());
					column.getColumn().setAlignment(SWT.CENTER);
				}
				else if (mCol == COL_LASTPLAYED) {
					layout.setColumnData(column.getColumn(), new ColumnPixelData(140, true, true));
					column.setLabelProvider(new DateLastPlayerLblProv());
				}
				else if (mCol == COL_HASH) {
					layout.setColumnData(column.getColumn(), new ColumnPixelData(90, true, true));
					column.setLabelProvider(new HashcodeLblProv());
					column.getColumn().setAlignment(SWT.CENTER);
				}
				else if (mCol == COL_MODIFIED) {
					layout.setColumnData(column.getColumn(), new ColumnPixelData(140, true, true));
					column.setLabelProvider(new DateLastModifiedLblProv());
				}
				else if (mCol == COL_DURATION) {
					layout.setColumnData(column.getColumn(), new ColumnPixelData(60, true, true));
					column.setLabelProvider(new DurationLblProv());
					column.getColumn().setAlignment(SWT.RIGHT);
				}
				else {
					throw new IllegalArgumentException();
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
	
	protected IAction toggleEnabledAction = new Action("Toggle enabled") {
		@Override
		public void run() {
			for (S track : getSelectedItems()) {
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

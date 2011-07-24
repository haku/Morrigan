package net.sparktank.morrigan.gui.editors.mmdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.gui.adaptors.DropMenuListener;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.editors.IMixedMediaItemDbEditor;
import net.sparktank.morrigan.gui.editors.MediaColumn;
import net.sparktank.morrigan.gui.preferences.MediaListPref;
import net.sparktank.morrigan.model.db.IDbColumn;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.IAbstractMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaItemDb.SortChangeListener;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayer2;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.IMixedMediaItemStorageLayer;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.morrigan.util.TimeHelper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public abstract class AbstractMixedMediaDbEditor<T extends IAbstractMixedMediaDb<T>>
		extends MixedMediaListEditor<T, IMixedMediaItem>
		implements IMixedMediaItemDbEditor<T, IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public AbstractMixedMediaDbEditor () {
		super();
	}
	
	@Override
	public void dispose() {
		getMediaList().unregisterSortChangeListener(this.sortChangeListener);
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	EditorPart methods.
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	@Override
	public void doSaveAs() {
		new MorriganMsgDlg("TODO: do save as for '" + getTitle() + "'.\n\n(this should not happen.)").open();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {/* NOT USED */}
	
	/**
	 * There is no need for the library to
	 * ever require the user manually save.
	 */
	@Override
	public boolean isDirty() {
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public T getMediaList () {
		return this.getEditorInput().getMediaList();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI components.
	
	Text txtFilter = null;
	private Button btnClearFilter = null;
	private Button btnProperties = null;
	private MenuManager prefMenuMgr = null;
	List<TypeFilterAction> typeFilterActions = new ArrayList<TypeFilterAction>();
	List<SortAction> sortActions = new ArrayList<SortAction>();
	
	@Override
	protected void createControls(Composite parent) {
		for (MediaType t : MediaType.values()) {
			TypeFilterAction a = new TypeFilterAction(t);
			a.setChecked(t == getMediaList().getDefaultMediaType());
			this.typeFilterActions.add(a);
		}
		
		List<IDbColumn> cols = getMediaList().getDbLayer().getMediaTblColumns();
		for (IDbColumn c : cols) {
			if (c.getHumanName() != null) {
    			SortAction a = new SortAction(c, SortDirection.ASC);
    			a.setChecked(c == getMediaList().getSort());
    			this.sortActions.add(a);
			}
		}
		getMediaList().registerSortChangeListener(this.sortChangeListener);
		
		// Pref menu.
		this.prefMenuMgr = new MenuManager();
		for (TypeFilterAction a : this.typeFilterActions) {
			this.prefMenuMgr.add(a);
		}
		this.prefMenuMgr.add(new Separator());
		for (SortAction a : this.sortActions) {
			this.prefMenuMgr.add(a);
		}
	}
	
	@Override
	protected List<Control> populateToolbar (Composite parent) {
		List<Control> ret = new LinkedList<Control>();
		
		this.txtFilter = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH);
		this.txtFilter.setMessage("Filter");
		this.txtFilter.addListener(SWT.Modify, this.filterListener);
		ret.add(this.txtFilter);
		
		this.btnClearFilter = new Button(parent, SWT.PUSH);
		this.btnClearFilter.setImage(getImageCache().readImage("icons/x.gif"));
		this.btnClearFilter.addSelectionListener(this.clearFilterListener);
		ret.add(this.btnClearFilter);
		
		this.btnProperties = new Button(parent, SWT.PUSH);
		this.btnProperties.setImage(getImageCache().readImage("icons/pref.gif"));
		this.btnProperties.addSelectionListener(new DropMenuListener(this.btnProperties, this.prefMenuMgr));
		ret.add(this.btnProperties);
		
		return ret;
	}
	
	@Override
	protected void populateContextMenu(List<IContributionItem> menu0, List<IContributionItem> menu1) {
		menu0.add(new ActionContributionItem(this.copyToAction));
		menu0.add(new ActionContributionItem(this.copyFilePath));
	}
	
	protected MenuManager getPrefMenuMgr () {
		return this.prefMenuMgr;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void listChanged () {
		if (this.lblStatus.isDisposed()) return;
		
		StringBuilder sb = new StringBuilder();
		
		DurationData d = getMediaList().getTotalDuration();
		
		sb.append(getMediaList().getCount());
		sb.append(" items totaling ");
		if (!d.isComplete()) {
			sb.append("more than ");
		}
		sb.append(TimeHelper.formatTimeSeconds(d.getDuration()));
		sb.append(".");
		
		long queryTime = getMediaList().getDurationOfLastRead();
		if (queryTime > 0) {
			sb.append("  Query took ");
			sb.append(TimeHelper.formatTimeMiliseconds(queryTime));
			sb.append(" seconds.");
		}
		
		this.lblStatus.setText(sb.toString());
	}
	
	private Listener filterListener = new Listener() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void handleEvent(Event event) {
			setFilterString(AbstractMixedMediaDbEditor.this.txtFilter.getText());
		}
	};
	
	SelectionAdapter clearFilterListener = new SelectionAdapter() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void widgetSelected(SelectionEvent e) {
			AbstractMixedMediaDbEditor.this.txtFilter.setText("");
			setFilterString("");
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	@Override
	protected boolean isSortable () {
		return true;
	}
	
	@Override
	protected void onSort (TableViewer table, TableViewerColumn column, int direction) {
		IDbColumn sort = getMediaList().getSort();
		MediaColumn mCol = parseMediaColumn(column.getColumn().getText());
		
		if (mCol == this.COL_FILE) {
			sort = IMediaItemStorageLayer2.SQL_TBL_MEDIAFILES_COL_FILE;
		}
		else if (mCol == this.COL_ADDED) {
			sort = IMediaItemStorageLayer2.SQL_TBL_MEDIAFILES_COL_DADDED;
		}
		else if (mCol == this.COL_HASH) {
			sort = IMediaItemStorageLayer2.SQL_TBL_MEDIAFILES_COL_MD5;
		}
		else if (mCol == this.COL_MODIFIED) {
			sort = IMediaItemStorageLayer2.SQL_TBL_MEDIAFILES_COL_DMODIFIED;
		}
		else if (mCol == this.COL_COUNTS) {
			sort = IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_STARTCNT;
		}
		else if (mCol == this.COL_LASTPLAYED) {
			sort = IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DLASTPLAY;
		}
		else if (mCol == this.COL_DURATION) {
			sort = IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DURATION;
		}
		else {
			throw new IllegalArgumentException();
		}
		
		SortDirection sortDir;
		if (direction==SWT.UP) {
			sortDir = SortDirection.ASC;
		} else {
			sortDir = SortDirection.DESC;
		}
		
		setSort(sort, sortDir);
	}
	
	void setSort (IDbColumn sort, SortDirection sortDir) {
		try {
			getMediaList().setSort(sort, sortDir);
		} catch (MorriganException e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}
	
	private SortChangeListener sortChangeListener = new SortChangeListener () {
		@SuppressWarnings("synthetic-access")
		@Override
		public void sortChanged(IDbColumn sort, SortDirection direction) {
			for (SortAction a : AbstractMixedMediaDbEditor.this.sortActions) {
				boolean c = sort == a.getSort();
				if (a.isChecked() != c) {
					a.setChecked(c);
				}
			}
			AbstractMixedMediaDbEditor.this.listChangeRrefresher.run();
		}
	};
	
	private class SortAction extends Action {
		
		private final IDbColumn sort;
		private final SortDirection sortDir;

		public SortAction (IDbColumn sort, SortDirection sortDir) {
			super("Sort by " + sort.getHumanName(), AS_RADIO_BUTTON);
			this.sort = sort;
			this.sortDir = sortDir;
		}
		
		public IDbColumn getSort() {
			return this.sort;
		}
		
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			super.run();
			if (isChecked()) {
				setSortMarker(null, SWT.NONE); // FIXME send actual column / direction.
				setSort(this.sort, this.sortDir);
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Type filtering.
	
	@Override
	protected List<MediaColumn> getColumns() {
		return getColumns(getMediaList().getDefaultMediaType());
	}
	
	protected List<MediaColumn> getColumns (MediaType mediaType) {
		MediaColumn[] cols;
		switch (mediaType) {
			case UNKNOWN:
				cols = this.COLS_UNKNOWN;
				break;
			case TRACK:
				cols = this.COLS_TRACKS;
				break;
			case PICTURE:
				cols = this.COLS_PICTURES;
				break;
			default: throw new IllegalArgumentException();
		}
		
		return Arrays.asList(cols);
	}
	
	@Override
	protected boolean isColumnVisible(MediaColumn col) {
		return MediaListPref.getColPref(this, col);
	}
	
	void setTypeFilter (MediaType filterType) {
		try {
			updateColumns(getColumns(filterType));
			getMediaList().setDefaultMediaType(filterType);
			refreshColumns();
		}
		catch (MorriganException e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}
	
	private class TypeFilterAction extends Action {
		
		private final MediaType filterType;

		public TypeFilterAction (MediaType filterType) {
			super("Filter by " + filterType.getHumanName(), AS_RADIO_BUTTON);
			this.filterType = filterType;
		}
		
		@Override
		public void run() {
			super.run();
			if (isChecked()) {
				setTypeFilter(this.filterType);
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

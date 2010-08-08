package net.sparktank.morrigan.gui.editors;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.DropMenuListener;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.DbColumn;
import net.sparktank.morrigan.model.MediaItemDb.SortChangeListener;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.tasks.MediaFileCopyTask;
import net.sparktank.morrigan.model.tracks.IMediaTrackList.DurationData;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.morrigan.model.tracks.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.tracks.library.LibrarySqliteLayer2;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public abstract class AbstractLibraryEditor<T extends AbstractMediaLibrary> extends MediaTrackListEditor<AbstractMediaLibrary, MediaTrack> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public AbstractLibraryEditor () {
		super();
	}
	
	@Override
	public void dispose() {
		getMediaList().unregisterSortChangeListener(this.sortChangeListener);
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected MediaTrack getNewS(String filePath) {
		return new MediaTrack(filePath);
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
	
	// FIXME how to remote the suppress warnings?
	@SuppressWarnings("unchecked")
	@Override
	public T getMediaList () {
		return (T) this.editorInput.getMediaList();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI components.
	
	List<SortAction> sortActions = new ArrayList<SortAction>();
	
	Text txtFilter = null;
	private Button btnClearFilter = null;
	private Button btnProperties = null;
	
	private MenuManager prefMenuMgr = null;
	
	@Override
	protected void createControls(Composite parent) {
		// Dependencies.
		
		List<DbColumn> cols = getMediaList().getDbLayer().getSqlTblMediaFilesColumns();
		
		for (DbColumn c : cols) {
			if (c.getHumanName() != null) {
    			SortAction a = new SortAction(c, SortDirection.ASC);
    			a.setChecked(c == getMediaList().getSort());
    			this.sortActions.add(a);
			}
		}
		getMediaList().registerSortChangeListener(this.sortChangeListener);
		
		// Pref menu.
		this.prefMenuMgr = new MenuManager();
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
		if (!d.complete) {
			sb.append("more than ");
		}
		sb.append(TimeHelper.formatTimeSeconds(d.duration));
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
		@Override
		public void handleEvent(Event event) {
			setFilterString(AbstractLibraryEditor.this.txtFilter.getText());
		}
	};
	
	SelectionAdapter clearFilterListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			AbstractLibraryEditor.this.txtFilter.setText("");
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
		DbColumn sort = getMediaList().getSort();
		MediaColumn mCol = parseMediaColumn(column.getColumn().getText());
		
		if (mCol == COL_FILE) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_FILE;
		}
		else if (mCol == COL_ADDED) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_DADDED;
		}
		else if (mCol == COL_COUNTS) {
			sort = LibrarySqliteLayer2.SQL_TBL_MEDIAFILES_COL_STARTCNT;
		}
		else if (mCol == COL_LASTPLAYED) {
			sort = LibrarySqliteLayer2.SQL_TBL_MEDIAFILES_COL_DLASTPLAY;
		}
		else if (mCol == COL_HASH) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_HASHCODE;
		}
		else if (mCol == COL_MODIFIED) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_DMODIFIED;
		}
		else if (mCol == COL_DURATION) {
			sort = LibrarySqliteLayer2.SQL_TBL_MEDIAFILES_COL_DURATION;
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
	
	void setSort (DbColumn sort, SortDirection sortDir) {
		try {
			getMediaList().setSort(sort, sortDir);
		} catch (MorriganException e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}
	
	private SortChangeListener sortChangeListener = new SortChangeListener () {
		@Override
		public void sortChanged(DbColumn sort, SortDirection direction) {
			for (SortAction a : AbstractLibraryEditor.this.sortActions) {
				boolean c = sort == a.getSort();
				if (a.isChecked() != c) {
					a.setChecked(c);
				}
			}
		}
	};
	
	private class SortAction extends Action {
		
		private final DbColumn sort;
		private final SortDirection sortDir;

		public SortAction (DbColumn sort, SortDirection sortDir) {
			super("Sort by " + sort.getHumanName(), AS_RADIO_BUTTON);
			this.sort = sort;
			this.sortDir = sortDir;
		}
		
		public DbColumn getSort() {
			return this.sort;
		}
		
		@Override
		public void run() {
			super.run();
			if (isChecked()) {
				setSortMarker(null, SWT.NONE); // FIXME send actual column / direction.
				setSort(this.sort, this.sortDir);
				System.out.println("sort by " + this.sort.toString());
			}
		}
		
	}
	
	String lastFileCopyTargetDir = null;
	
	protected IAction copyToAction = new Action("Copy to...") {
		@Override
		public void run () {
			ArrayList<MediaTrack> selectedTracks = getSelectedItems();
			
			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Copy Files...");
			dlg.setMessage("Select a directory to copy files to.");
			if (AbstractLibraryEditor.this.lastFileCopyTargetDir != null) {
				dlg.setFilterPath(AbstractLibraryEditor.this.lastFileCopyTargetDir);
			}
			String dir = dlg.open();
			
			if (dir != null) {
				AbstractLibraryEditor.this.lastFileCopyTargetDir = dir;
				
				MediaFileCopyTask<MediaTrack> task = new MediaFileCopyTask<MediaTrack>(getMediaList(), selectedTracks, new File(dir));
				TaskJob job = new TaskJob(task, getSite().getShell().getDisplay());
				job.schedule();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

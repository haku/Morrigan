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
import net.sparktank.morrigan.model.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.library.AbstractMediaLibrary.SortChangeListener;
import net.sparktank.morrigan.model.library.MediaLibraryTrack;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySortDirection;
import net.sparktank.morrigan.model.tasks.MediaFileCopyTask;
import net.sparktank.morrigan.model.tracks.MediaTrackList.DurationData;

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

public abstract class AbstractLibraryEditor<T extends AbstractMediaLibrary> extends MediaTrackListEditor<AbstractMediaLibrary, MediaLibraryTrack> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public AbstractLibraryEditor () {
		super();
	}
	
	@Override
	public void dispose() {
		getMediaList().unregisterSortChangeListener(sortChangeListener);
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
	public void doSave(IProgressMonitor monitor) {}
	
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
		if (editorInput.getMediaList() instanceof AbstractMediaLibrary) {
			return (T) editorInput.getMediaList();
			
		} else {
			throw new IllegalArgumentException();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI components.
	
	private List<SortAction> sortActions = new ArrayList<SortAction>();
	
	private Text txtFilter = null;
	private Button btnClearFilter = null;
	private Button btnProperties = null;
	
	private MenuManager prefMenuMgr = null;
	
	@Override
	protected void createControls(Composite parent) {
		// Dependencies.
		
		for (LibrarySort s : LibrarySort.values()) {
			SortAction a = new SortAction(s, LibrarySortDirection.ASC);
			a.setChecked(s == getMediaList().getSort());
			sortActions.add(a);
		}
		getMediaList().registerSortChangeListener(sortChangeListener);
		
		// Pref menu.
		prefMenuMgr = new MenuManager();
		for (SortAction a : sortActions) {
			prefMenuMgr.add(a);
		}
	}
	
	@Override
	protected List<Control> populateToolbar (Composite parent) {
		List<Control> ret = new LinkedList<Control>();
		
		txtFilter = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH);
		txtFilter.setMessage("Filter");
		txtFilter.addListener(SWT.Modify, filterListener);
		ret.add(txtFilter);
		
		btnClearFilter = new Button(parent, SWT.PUSH);
		btnClearFilter.setImage(getImageCache().readImage("icons/x.gif"));
		btnClearFilter.addSelectionListener(clearFilterListener);
		ret.add(btnClearFilter);
		
		btnProperties = new Button(parent, SWT.PUSH);
		btnProperties.setImage(getImageCache().readImage("icons/pref.gif"));
		btnProperties.addSelectionListener(new DropMenuListener(btnProperties, prefMenuMgr));
		ret.add(btnProperties);
		
		return ret;
	}
	
	@Override
	protected void populateContextMenu(List<IContributionItem> menu0, List<IContributionItem> menu1) {
		menu0.add(new ActionContributionItem(copyToAction));
	}
	
	protected MenuManager getPrefMenuMgr () {
		return prefMenuMgr;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void listChanged () {
		if (lblStatus.isDisposed()) return;
		
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
		
		lblStatus.setText(sb.toString());
	}
	
	private Listener filterListener = new Listener() {
		public void handleEvent(Event event) {
			setFilterString(txtFilter.getText());
		}
	};
	
	SelectionAdapter clearFilterListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			txtFilter.setText("");
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
		LibrarySort sort = getMediaList().getSort();
		MediaColumn mCol = parseMediaColumn(column.getColumn().getText());
		switch (mCol) {
			case FILE:
				sort = LibrarySort.FILE;
				break;
			
			case DADDED:
				sort = LibrarySort.DADDED;
				break;
				
			case COUNTS:
				sort = LibrarySort.STARTCNT;
				break;
				
			case DLASTPLAY:
				sort = LibrarySort.DLASTPLAY;
				break;
				
			case HASHCODE:
				sort = LibrarySort.HASHCODE;
				break;
				
			case DMODIFIED:
				sort = LibrarySort.DMODIFIED;
				break;
				
			case DURATION:
				sort = LibrarySort.DURATION;
				break;
				
			default:
				throw new IllegalArgumentException();
		}
		
		LibrarySortDirection sortDir;
		if (direction==SWT.UP) {
			sortDir = LibrarySortDirection.ASC;
		} else {
			sortDir = LibrarySortDirection.DESC;
		}
		
		setSort(sort, sortDir);
	}
	
	private void setSort (LibrarySort sort, LibrarySortDirection sortDir) {
		try {
			getMediaList().setSort(sort, sortDir);
		} catch (MorriganException e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}
	
	private SortChangeListener sortChangeListener = new SortChangeListener () {
		@Override
		public void sortChanged(LibrarySort sort, LibrarySortDirection direction) {
			for (SortAction a : sortActions) {
				boolean c = sort == a.getSort();
				if (a.isChecked() != c) {
					a.setChecked(c);
				}
			}
		}
	};
	
	private class SortAction extends Action {
		
		private final LibrarySort sort;
		private final LibrarySortDirection sortDir;

		public SortAction (LibrarySort sort, LibrarySortDirection sortDir) {
			super("Sort by " + sort.toString(), AS_RADIO_BUTTON);
			this.sort = sort;
			this.sortDir = sortDir;
		}
		
		public LibrarySort getSort() {
			return sort;
		}
		
		@Override
		public void run() {
			super.run();
			if (isChecked()) {
				setSortMarker(null, SWT.NONE); // FIXME send actual column / direction.
				setSort(sort, sortDir);
				System.out.println("sort by " + sort.toString());
			}
		}
		
	}
	
	private String lastFileCopyTargetDir = null;
	
	protected IAction copyToAction = new Action("Copy to...") {
		public void run () {
			ArrayList<MediaLibraryTrack> selectedTracks = getSelectedTracks();
			
			DirectoryDialog dlg = new DirectoryDialog(getSite().getShell());
			dlg.setText("Copy Files...");
			dlg.setMessage("Select a directory to copy files to.");
			if (lastFileCopyTargetDir != null) {
				dlg.setFilterPath(lastFileCopyTargetDir);
			}
			String dir = dlg.open();
			
			if (dir != null) {
				lastFileCopyTargetDir = dir;
				
				MediaFileCopyTask<MediaLibraryTrack> task = new MediaFileCopyTask<MediaLibraryTrack>(getMediaList(), selectedTracks, new File(dir));
				TaskJob job = new TaskJob(task, getSite().getShell().getDisplay());
				job.schedule();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

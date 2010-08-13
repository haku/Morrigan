package net.sparktank.morrigan.gui.editors.pictures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.DropMenuListener;
import net.sparktank.morrigan.gui.editors.IMediaItemDbEditor;
import net.sparktank.morrigan.gui.editors.MediaColumn;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.db.impl.DbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb.SortChangeListener;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer.SortDirection;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.gallery.AbstractGallery;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public abstract class AbstractGalleryEditor<T extends AbstractGallery> extends MediaPictureListEditor<T, MediaPicture> implements IMediaItemDbEditor {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public AbstractGalleryEditor () {
		super();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected MediaPicture getNewS(String filePath) {
		return new MediaPicture(filePath);
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
	List<SortAction> sortActions = new ArrayList<SortAction>();
	
	@Override
	protected void createControls(Composite parent) {
		List<DbColumn> cols = getMediaList().getDbLayer().getSqlTblMediaFilesColumns();
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
		
		sb.append(getMediaList().getCount());
		sb.append(" items.");
		
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
			setFilterString(AbstractGalleryEditor.this.txtFilter.getText());
		}
	};
	
	SelectionAdapter clearFilterListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			AbstractGalleryEditor.this.txtFilter.setText("");
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
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_FILE;
		}
		else if (mCol == this.COL_ADDED) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_DADDED;
		}
		else if (mCol == this.COL_HASH) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_HASHCODE;
		}
		else if (mCol == this.COL_MODIFIED) {
			sort = MediaSqliteLayer2.SQL_TBL_MEDIAFILES_COL_DMODIFIED;
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
		@Override
		public void sortChanged(IDbColumn sort, SortDirection direction) {
			for (SortAction a : AbstractGalleryEditor.this.sortActions) {
				boolean c = sort == a.getSort();
				if (a.isChecked() != c) {
					a.setChecked(c);
				}
			}
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

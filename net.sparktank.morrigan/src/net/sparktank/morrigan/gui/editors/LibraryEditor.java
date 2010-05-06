package net.sparktank.morrigan.gui.editors;

import java.util.ArrayList;
import java.util.List;
import net.sparktank.morrigan.model.library.*;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.ApplicationActionBarAdvisor;
import net.sparktank.morrigan.gui.actions.LibraryUpdateAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.ActionListener;
import net.sparktank.morrigan.gui.display.DropMenuListener;
import net.sparktank.morrigan.gui.views.ViewLibraryProperties;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.MediaList.DurationData;
import net.sparktank.morrigan.model.library.AbstractLibrary;
import net.sparktank.morrigan.model.library.AbstractLibrary.SortChangeListener;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySortDirection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;

public class LibraryEditor extends MediaListEditor<AbstractLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryEditor () {
		super();
	}
	
	@Override
	public void dispose() {
		getMediaList().unregisterSortChangeListener(sortChangeListener);
		disposeIcons();
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	EditorPart methods.
	
	@Override
	public void setFocus() {
		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ACTIONID_ADD, addAction);
		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ACTIONID_SHOWPROPERTIES, showPropertiesAction);
	}
	
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
//	GUI components.
	
	private Image iconX;
	private Image iconQueueAdd;
	private Image iconAdd;
	private Image iconProperties;
	
	private void makeIcons () {
		iconX = Activator.getImageDescriptor("icons/x.gif").createImage();
		iconQueueAdd = Activator.getImageDescriptor("icons/queue-add.gif").createImage();
		iconAdd = Activator.getImageDescriptor("icons/plus.gif").createImage();
		iconProperties = Activator.getImageDescriptor("icons/pref.gif").createImage();
	}
	
	private void disposeIcons () {
		if (iconX != null) iconX.dispose();
		if (iconQueueAdd != null) iconQueueAdd.dispose();
		if (iconAdd != null) iconAdd.dispose();
		if (iconProperties != null) iconProperties.dispose();
	}
	
	private List<SortAction> sortActions = new ArrayList<SortAction>();
	
	private Label lblStatus = null;
	private Text txtFilter = null;
	
	@Override
	protected void populateToolbar (Composite parent) {
		// Dependencies.
		
		makeIcons();
		
		for (LibrarySort s : LibrarySort.values()) {
			SortAction a = new SortAction(s, LibrarySortDirection.ASC);
			a.setChecked(s == getMediaList().getSort());
			sortActions.add(a);
		}
		getMediaList().registerSortChangeListener(sortChangeListener);
		
		// Off-screen controls.
		
		// Pref menu.
		MenuManager prefMenuMgr = new MenuManager();
		for (SortAction a : sortActions) {
			prefMenuMgr.add(a);
		}
		/* FIXME
		 * Instead of checking the type, create separate editors
		 * for remote and local libraries? 
		 */
		if (getMediaList() instanceof MediaLibrary) {
			MediaLibrary ml = (MediaLibrary) getMediaList();
			
			prefMenuMgr.add(new Separator());
			prefMenuMgr.add(new LibraryUpdateAction(ml));
			prefMenuMgr.add(showPropertiesAction);
		}
		
		// Context menu.
		MenuManager contextMenuMgr = new MenuManager();
		contextMenuMgr.add(addToQueueAction);
		contextMenuMgr.add(getAddToMenu());
		contextMenuMgr.add(new Separator());
		contextMenuMgr.add(toggleEnabledAction);
		contextMenuMgr.add(removeAction);
		setTableMenu(contextMenuMgr.createContextMenu(parent));
		
		// On-screen controls.
		
		final int sep = 3;
		FormData formData;
		
		lblStatus = new Label(parent, SWT.NONE);
		txtFilter = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH);
		Button btnClearFilter = new Button(parent, SWT.PUSH);
		Button btnAddToQueue = new Button(parent, SWT.PUSH);
		Button btnAdd = new Button(parent, SWT.PUSH);
		Button btnProperties = new Button(parent, SWT.PUSH);
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(0, sep*2);
		formData.right = new FormAttachment(txtFilter, -sep);
		lblStatus.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnClearFilter, -sep);
		txtFilter.setLayoutData(formData);
		txtFilter.setMessage("Filter");
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnAddToQueue, -sep * 3);
		btnClearFilter.setImage(iconX);
		btnClearFilter.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnAdd, -sep);
		btnAddToQueue.setImage(iconQueueAdd);
		btnAddToQueue.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnProperties, -sep);
		btnAdd.setImage(iconAdd);
		btnAdd.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(100, -sep);
		btnProperties.setImage(iconProperties);
		btnProperties.setLayoutData(formData);
		
		txtFilter.addSelectionListener(filterListener);
		btnClearFilter.addSelectionListener(clearFilterListener);
		btnAddToQueue.addSelectionListener(new ActionListener(addToQueueAction));
		btnAdd.addSelectionListener(new ActionListener(addAction));
		btnProperties.addSelectionListener(new DropMenuListener(btnProperties, prefMenuMgr));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void listChanged () {
		if (lblStatus.isDisposed()) return;
		
		DurationData d = getMediaList().getTotalDuration();
		
		lblStatus.setText(
				getMediaList().getCount() + " items"
				+ " totaling " + (d.complete ? "" : "more than ") +
				TimeHelper.formatTime(d.duration) + "."
				);
	}
	
	private SelectionAdapter filterListener = new SelectionAdapter() {
		public void widgetDefaultSelected(SelectionEvent e) {
			if (e.detail == SWT.CANCEL) {
				clearFilterListener.widgetSelected(null);
			} else {
				setFilterString(txtFilter.getText());
			}
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
	
	protected void setSort (LibrarySort sort, LibrarySortDirection sortDir) {
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
	
	protected class SortAction extends Action {
		
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private IAction addAction = new Action("Add") {
		public void run () {
			ViewLibraryProperties propView = showLibPropView();
			if (propView!=null) {
				propView.showAddDlg(true);
			}
		}
	};
	
	private IAction showPropertiesAction = new Action("Properties") {
		public void run () {
			showLibPropView();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected ViewLibraryProperties showLibPropView () {
		try {
			IViewPart showView = getSite().getPage().showView(ViewLibraryProperties.ID);
			ViewLibraryProperties viewProp = (ViewLibraryProperties) showView;
			/* FIXME
			 * Instead of checking the type, create separate editors
			 * for remote and local libraries? 
			 */
			if (getMediaList() instanceof MediaLibrary) {
				MediaLibrary ml = (MediaLibrary) getMediaList();
				viewProp.setContent(ml);
			}
			return viewProp;
			
		} catch (Exception e) {
			new MorriganMsgDlg(e).open();
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

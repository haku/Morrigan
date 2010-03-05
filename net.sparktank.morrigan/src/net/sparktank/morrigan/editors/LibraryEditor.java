package net.sparktank.morrigan.editors;


import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.ApplicationActionBarAdvisor;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.display.ActionListener;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySortDirection;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.views.ViewLibraryProperties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;

public class LibraryEditor extends MediaListEditor<MediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.editors.LibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryEditor () {
		super();
	}
	
	@Override
	public void dispose() {
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
	
	private Image iconAdd;
	private Image iconRemove;
	private Image iconProperties;
	
	private void makeIcons () {
		iconAdd = Activator.getImageDescriptor("icons/plus.gif").createImage();
		iconRemove = Activator.getImageDescriptor("icons/minus.gif").createImage();
		iconProperties = Activator.getImageDescriptor("icons/pref.gif").createImage();
	}
	
	private void disposeIcons () {
		iconAdd.dispose();
		iconRemove.dispose();
		iconProperties.dispose();
	}
	
	private Label lblStatus;
	
	@Override
	protected void populateToolbar (Composite parent) {
		makeIcons();
		
		final int sep = 3;
		FormData formData;
		
		lblStatus = new Label(parent, SWT.NONE);
		Button btnAdd = new Button(parent, SWT.PUSH);
		Button btnRemove = new Button(parent, SWT.PUSH);
		Button btnProperties = new Button(parent, SWT.PUSH);
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(0, sep*2);
		formData.right = new FormAttachment(btnAdd, -sep);
		lblStatus.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnRemove, -sep);
		btnAdd.setImage(iconAdd);
		btnAdd.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnProperties, -sep);
		btnRemove.setImage(iconRemove);
		btnRemove.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(100, -sep);
		btnProperties.setImage(iconProperties);
		btnProperties.setLayoutData(formData);
		
		btnAdd.addSelectionListener(new ActionListener(addAction));
		btnRemove.addSelectionListener(new ActionListener(removeAction));
		btnProperties.addSelectionListener(new ActionListener(showPropertiesAction));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void listChanged () {
		if (lblStatus.isDisposed()) return;
		
		lblStatus.setText(
				getEditedMediaList().getCount() + " items."
				);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	@Override
	protected boolean isSortable () {
		return true;
	}
	
	@Override
	protected void onSort (TableViewer table, TableViewerColumn column, int direction) {
		LibrarySort sort = getEditedMediaList().getSort();
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
		
		try {
			getEditedMediaList().setSort(sort, sortDir);
		} catch (MorriganException e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private IAction addAction = new Action("add") {
		public void run () {
			ViewLibraryProperties propView = showLibPropView();
			if (propView!=null) {
				propView.showAddDlg(true);
			}
		}
	};
	
	private IAction removeAction = new Action("remove") {
		public void run () {
			MorriganMsgDlg dlg = new MorriganMsgDlg("Remove selected from " + getTitle() + "?", MorriganMsgDlg.YESNO);
			dlg.open();
			if (dlg.getReturnCode() == MorriganMsgDlg.OK) {
				for (MediaItem track : getSelectedTracks()) {
					try {
						removeTrack(track);
					} catch (MorriganException e) {
						// TODO something more useful here.
						e.printStackTrace();
					}
				}
			}
		}
	};
	
	private IAction showPropertiesAction = new Action("showProperties") {
		public void run () {
			showLibPropView();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected ViewLibraryProperties showLibPropView () {
		try {
			IViewPart showView = getSite().getPage().showView(ViewLibraryProperties.ID);
			ViewLibraryProperties viewProp = (ViewLibraryProperties) showView;
			viewProp.setContent(getEditedMediaList());
			return viewProp;
			
		} catch (Exception e) {
			new MorriganMsgDlg(e).open();
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

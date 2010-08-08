package net.sparktank.morrigan.gui.editors.pictures;

import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.display.DropMenuListener;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.gallery.AbstractGallery;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class AbstractGalleryEditor<T extends AbstractGallery> extends MediaPictureListEditor<AbstractGallery, MediaPicture> {
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
		return (T) this.getEditorInput().getMediaList();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI components.
	
	private Button btnProperties = null;
	private MenuManager prefMenuMgr = null;
	
	@Override
	protected void createControls(Composite parent) {
		// TODO
		
		// Pref menu.
		this.prefMenuMgr = new MenuManager();
	}
	
	@Override
	protected List<Control> populateToolbar (Composite parent) {
		List<Control> ret = new LinkedList<Control>();
		
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sorting.
	
	@Override
	protected boolean isSortable () {
		return true;
	}
	
	@Override
	protected void onSort (TableViewer table, TableViewerColumn column, int direction) {
		// TODO setSort()...
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

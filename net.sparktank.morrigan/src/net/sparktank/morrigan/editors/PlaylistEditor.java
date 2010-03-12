package net.sparktank.morrigan.editors;

import java.io.File;
import java.util.logging.Logger;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.ApplicationActionBarAdvisor;
import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.display.ActionListener;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.MediaPlaylist;

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

public class PlaylistEditor extends MediaListEditor<MediaPlaylist> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.editors.PlaylistEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlaylistEditor () {
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
		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.REVERT.getId(), revertAction);
		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ACTIONID_ADD, addAction);
		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ACTIONID_REMOVE, removeAction);
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	@Override
	public void doSaveAs() {
		new MorriganMsgDlg("TODO: do save as for '" + getTitle() + "'.\n\n(this should not happen.)").open();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			getMediaList().writeToFile();
		} catch (MorriganException e) {
			new MorriganMsgDlg(e);
		}
	}
	
	@Override
	protected boolean isSortable() {
		return false;
	}
	
	@Override
	protected void onSort(TableViewer table, TableViewerColumn column, int direction) {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI components.
	
	private Image iconQueueAdd;
	private Image iconAdd;
	private Image iconRemove;
	private Image iconProperties;
	private Image iconSave;
	
	private void makeIcons () {
		iconQueueAdd = Activator.getImageDescriptor("icons/queue-add.gif").createImage();
		iconAdd = Activator.getImageDescriptor("icons/plus.gif").createImage();
		iconRemove = Activator.getImageDescriptor("icons/minus.gif").createImage();
		iconProperties = Activator.getImageDescriptor("icons/pref.gif").createImage();
		iconSave = Activator.getImageDescriptor("icons/save.gif").createImage();
	}
	
	private void disposeIcons () {
		iconQueueAdd.dispose();
		iconAdd.dispose();
		iconRemove.dispose();
		iconProperties.dispose();
		iconSave.dispose();
	}
	
	private Label lblStatus;
	
	@Override
	protected void populateToolbar(Composite parent) {
		// Dependencies.
		
		makeIcons();
		
		// On-screen controls.
		
		final int sep = 3;
		FormData formData;
		
		Button btnAddToQueue = new Button(parent, SWT.PUSH);
		lblStatus = new Label(parent, SWT.NONE);
		Button btnAdd = new Button(parent, SWT.PUSH);
		Button btnRemove = new Button(parent, SWT.PUSH);
		Button btnSave = new Button(parent, SWT.PUSH);
		
		formData = new FormData();
		formData.top = new FormAttachment(50, -(lblStatus.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(0, sep*2);
		formData.right = new FormAttachment(btnAddToQueue, -sep);
		lblStatus.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnAdd, -sep);
		btnAddToQueue.setImage(iconQueueAdd);
		btnAddToQueue.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnRemove, -sep);
		btnAdd.setImage(iconAdd);
		btnAdd.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnSave, -sep);
		btnRemove.setImage(iconRemove);
		btnRemove.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(100, -sep);
		btnSave.setImage(iconSave);
		btnSave.setLayoutData(formData);
		
		btnAddToQueue.addSelectionListener(new ActionListener(addToQueueAction));
		btnAdd.addSelectionListener(new ActionListener(addAction));
		btnRemove.addSelectionListener(new ActionListener(removeAction));
		btnSave.addSelectionListener(new ActionListener(new SaveEditorAction(this)));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected void listChanged () {
		if (lblStatus.isDisposed()) return;
		
		lblStatus.setText(
				getMediaList().getCount() + " items."
				);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private IAction revertAction = new Action("revert") {
		public void run () {
			new MorriganMsgDlg("TODO: figure out how to implement revert desu~.").open();
		}
	};
	
	private IAction addAction = new Action("add") {
		public void run () {
			String[] supportedFormats;
			try {
				supportedFormats = Config.getMediaFileTypes();
			} catch (MorriganException e) {
				new MorriganMsgDlg(e).open();
				return;
			}
			
			String[] filterList = new String[supportedFormats.length+2];
			StringBuilder allTypes = new StringBuilder();
			for (int i = 0; i < supportedFormats.length; i++) {
				filterList[i+1] = "*." + supportedFormats[i];
				
				if (i>0) allTypes.append(";");
				allTypes.append(filterList[i+1]);
			}
			filterList[0] = allTypes.toString();
			filterList[filterList.length-1] = "*.*";
			
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			
			FileDialog dialog = new FileDialog(shell, SWT.MULTI);
			dialog.setText("Add to " + getTitle());
			dialog.setFilterNames(filterList);
			dialog.setFilterExtensions(filterList);
			
			String firstSel = dialog.open();
			if (firstSel != null) {
				File firstSelFile = new File(firstSel);
				String baseDir = firstSelFile.getAbsoluteFile().getParentFile().getAbsolutePath();
				
				String[] files = dialog.getFileNames();
				int n = 0;
				for (String file : files) {
					String toAdd = baseDir + File.separatorChar + file;
					addTrack(toAdd);
					n++;
				}
				logger.fine("Added " + n + " file to '" + getTitle() + "'.");
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

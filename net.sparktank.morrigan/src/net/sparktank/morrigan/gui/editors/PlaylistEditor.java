package net.sparktank.morrigan.gui.editors;

import java.io.File;
import java.util.logging.Logger;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.ApplicationActionBarAdvisor;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.display.ActionListener;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList.DurationData;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

public class PlaylistEditor extends MediaListEditor<MediaPlaylist,MediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.PlaylistEditor";
	
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
	
	@Override
	protected boolean handleReadError(Exception e) {
		return false;
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
	
	private Image iconX;
	private Image iconQueueAdd;
	private Image iconAdd;
	private Image iconProperties;
	private Image iconSave;
	
	private void makeIcons () {
		iconX = Activator.getImageDescriptor("icons/x.gif").createImage();
		iconQueueAdd = Activator.getImageDescriptor("icons/queue-add.gif").createImage();
		iconAdd = Activator.getImageDescriptor("icons/plus.gif").createImage();
		iconProperties = Activator.getImageDescriptor("icons/pref.gif").createImage();
		iconSave = Activator.getImageDescriptor("icons/save.gif").createImage();
	}
	
	private void disposeIcons () {
		iconX.dispose();
		iconQueueAdd.dispose();
		iconAdd.dispose();
		iconProperties.dispose();
		iconSave.dispose();
	}
	
	private Label lblStatus;
	private Text txtFilter;
	
	@Override
	protected void populateToolbar(Composite parent) {
		// Dependencies.
		
		makeIcons();
		
		// Off-screen controls.
		
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
		Button btnSave = new Button(parent, SWT.PUSH);
		
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
		formData.right = new FormAttachment(btnSave, -sep);
		btnAdd.setImage(iconAdd);
		btnAdd.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(100, -sep);
		btnSave.setImage(iconSave);
		btnSave.setLayoutData(formData);
		
		txtFilter.addSelectionListener(filterListener);
		btnClearFilter.addSelectionListener(clearFilterListener);
		btnAddToQueue.addSelectionListener(new ActionListener(addToQueueAction));
		btnAdd.addSelectionListener(new ActionListener(addAction));
		btnSave.addSelectionListener(new ActionListener(new SaveEditorAction(this)));
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
				TimeHelper.formatTimeSeconds(d.duration) + "."
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
//	Actions.
	
	private IAction revertAction = new Action("revert") {
		public void run () {
			new MorriganMsgDlg("TODO: figure out how to implement revert desu~.").open();
		}
	};
	
	private IAction addAction = new Action("add") {
		
		private String lastDir = null;
		
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
			dialog.setFilterPath(lastDir);
			
			String firstSel = dialog.open();
			if (firstSel != null) {
				File firstSelFile = new File(firstSel);
				String baseDir = firstSelFile.getAbsoluteFile().getParentFile().getAbsolutePath();
				lastDir = baseDir;
				
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

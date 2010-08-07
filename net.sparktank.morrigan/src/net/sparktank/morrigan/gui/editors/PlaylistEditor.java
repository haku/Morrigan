package net.sparktank.morrigan.gui.editors;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.display.ActionListener;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.tracks.IMediaTrackList.DurationData;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.morrigan.model.tracks.playlist.MediaPlaylist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class PlaylistEditor extends MediaTrackListEditor<MediaPlaylist,MediaTrack> {
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
	
//	@Override
//	public void setFocus() {
//		getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.REVERT.getId(), revertAction);
//		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ACTIONID_ADD, addAction);
//		getEditorSite().getActionBars().setGlobalActionHandler(ApplicationActionBarAdvisor.ACTIONID_REMOVE, removeAction);
//	}
	
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
	
	private Text txtFilter;
	
	@Override
	protected void createControls(Composite parent) {
		//	Dependencies.
		makeIcons();
	}
	
	@Override
	protected List<Control> populateToolbar(Composite parent) {
		List<Control> ret = new LinkedList<Control>();
		
		txtFilter = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH);
		txtFilter.setMessage("Filter");
		txtFilter.addSelectionListener(filterListener);
		ret.add(txtFilter);
		
		Button btnClearFilter = new Button(parent, SWT.PUSH);
		btnClearFilter.setImage(iconX);
		btnClearFilter.addSelectionListener(clearFilterListener);
		ret.add(btnClearFilter);
		
		Button btnAddToQueue = new Button(parent, SWT.PUSH);
		btnAddToQueue.setImage(iconQueueAdd);
		btnAddToQueue.addSelectionListener(new ActionListener(addToQueueAction));
		ret.add(btnAddToQueue);
		
		Button btnAdd = new Button(parent, SWT.PUSH);
		btnAdd.setImage(iconAdd);
		btnAdd.addSelectionListener(new ActionListener(addAction));
		ret.add(btnAdd);
		
		Button btnSave = new Button(parent, SWT.PUSH);
		btnSave.setImage(iconSave);
		btnSave.addSelectionListener(new ActionListener(new SaveEditorAction(this)));
		ret.add(btnSave);
		
		return ret;
	}
	
	@Override
	protected void populateContextMenu(List<IContributionItem> menu0, List<IContributionItem> menu1) {
		menu0.add(new ActionContributionItem(addToQueueAction));
		menu0.add(getAddToMenu());
		
		menu1.add(new ActionContributionItem(toggleEnabledAction));
		menu1.add(new ActionContributionItem(removeAction));
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
	
//	private IAction revertAction = new Action("revert") {
//		public void run () {
//			new MorriganMsgDlg("TODO: figure out how to implement revert desu~.").open();
//		}
//	};
	
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

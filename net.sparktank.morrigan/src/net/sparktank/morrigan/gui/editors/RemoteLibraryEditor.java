package net.sparktank.morrigan.gui.editors;

import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.jobs.RefreshLibraryJob;
import net.sparktank.morrigan.model.library.remote.RemoteMediaLibrary;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


public class RemoteLibraryEditor extends AbstractLibraryEditor<RemoteMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.RemoteLibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.
	
	protected List<Control> populateToolbar(Composite parent) {
		List<Control> ret = super.populateToolbar(parent);
		
		Button btnRefresh = new Button(parent, SWT.PUSH);
		btnRefresh.setText("Refresh");
		btnRefresh.addSelectionListener(refreshListener);
		ret.add(ret.size() - 1, btnRefresh);
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected boolean handleReadError(Exception e) {
		new MorriganMsgDlg(e).open();
		return true;
	}
	
	@Override
	protected void readInputData() throws MorriganException {
		getMediaList().readFromCache();
		RefreshLibraryJob job = RefreshLibraryJob.FACTORY.manufacture(getMediaList());
		if (job != null) job.schedule(3000);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions and listeners.
	
	private SelectionAdapter refreshListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			try {
				RefreshLibraryJob job = RefreshLibraryJob.FACTORY.manufacture(getMediaList());
				if (job != null) {
					job.schedule();
				} else {
					new MorriganMsgDlg("Refresh for '"+getMediaList().getListName()+"' already running.").open();
				}
				
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

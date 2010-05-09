package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.jobs.RefreshLibraryJob;
import net.sparktank.morrigan.model.library.RemoteMediaLibrary;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


public class RemoteLibraryEditor extends AbstractLibraryEditor<RemoteMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.RemoteLibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected boolean handleReadError(Exception e) {
		new MorriganMsgDlg(e).open();
		return true;
	}
	
	@Override
	protected void readInputData() throws MorriganException {
		RefreshLibraryJob job = new RefreshLibraryJob(getMediaList());
		job.schedule();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected void populateToolbar(Composite parent) {
		super.populateToolbar(parent);
		
		Button btnRefresh = new Button(parent, SWT.PUSH);
		
		btnAdd.setVisible(false);
		FormData formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnRefresh, -sep);
		btnAddToQueue.setImage(iconQueueAdd);
		btnAddToQueue.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		formData.right = new FormAttachment(btnProperties, -sep);
		btnRefresh.setText("Refresh");
		btnRefresh.setLayoutData(formData);
		btnRefresh.addSelectionListener(refreshListener);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private SelectionAdapter refreshListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			try {
				readInputData();
				
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

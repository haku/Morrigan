package net.sparktank.morrigan.dialogs;

import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.media.PlayItem;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class JumpToDlg extends Dialog {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = -6047405753309652452L;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaLibrary mediaLibrary;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public JumpToDlg (Shell parent, MediaLibrary mediaLibrary) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		this.mediaLibrary = mediaLibrary;
		setText("Jump to track");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static int SEP = 3;
	
	private Text text;
	private PlayItem returnValue = null;
	
	public PlayItem open () {
		FormData formData;
		
		final Shell shell = new Shell(getParent(), getStyle());
		shell.setLayout(new FormLayout());
		
		text = new Text(shell, SWT.SINGLE | SWT.BORDER);
		TableViewer tableViewer =  new TableViewer(shell, SWT.V_SCROLL);
		Button btnOk = new Button(shell, SWT.PUSH);
		Button btnCancel = new Button(shell, SWT.PUSH);
		
		shell.setDefaultButton(btnOk);
		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				switch (event.detail) {
					case SWT.TRAVERSE_ESCAPE:
						shell.close();
						event.detail = SWT.TRAVERSE_NONE;
						event.doit = false;
						break;
					
				}
			}
		});
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		text.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(text, SEP);
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(btnOk, -SEP);
		formData.width = 300;
		formData.height = 300;
		tableViewer.getTable().setLayoutData(formData);
		
		btnOk.setText("Ok");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnOk.setLayoutData(formData);
		
		btnCancel.setText("Cancel");
		formData = new FormData();
		formData.right = new FormAttachment(btnOk, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnCancel.setLayoutData(formData);
		
		text.addVerifyListener(textChangeListener);
		tableViewer.setContentProvider(contentProvider);
		btnOk.addSelectionListener(buttonListener);
		btnCancel.addSelectionListener(buttonListener);
		
		shell.pack();
		shell.open();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		return returnValue;
	}
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			
			// TODO.
			return new String[]{};
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	private SelectionListener buttonListener = new SelectionAdapter() {
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button b = (Button) e.widget;
			if (b.getShell().getDefaultButton() == b) {
				System.out.println("ok desu~");
				
				returnValue = null; // TODO fill in selected value.
				
			} else {
				System.out.println("cancel desu~");
			}
			b.getShell().close();
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Object searchLock = new Object();
	private volatile boolean searchRunning = false;
	private volatile boolean searchDirty = false;
	
	private VerifyListener textChangeListener = new VerifyListener() {
		@Override
		public void verifyText(VerifyEvent e) {
			synchronized (searchLock) {
				if (!searchRunning) {
					updateSearchResults();
					searchRunning = true;
				} else {
					searchDirty = true;
				}
			}
		}
	};
	
	private void updateSearchResults () {
		getParent().getDisplay().asyncExec(updateSearchResults);
	}
	
	private Runnable updateSearchResults = new Runnable() {
		@Override
		public void run() {
			updateSearchResults(text.getText());
		}
	};
	
	private void updateSearchResults (final String query) {
		Thread t = new Thread() {
			@Override
			public void run() {
				doSearch(query);
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	private void doSearch (String query) {
		System.out.println("Searching for '" + query + "'...");
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished searching for '" + query + "'.");
		
		synchronized (searchLock) {
			if (searchDirty) {
				updateSearchResults();
				searchDirty = false;
			} else {
				searchRunning = false;
			}
		}
	}
	

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

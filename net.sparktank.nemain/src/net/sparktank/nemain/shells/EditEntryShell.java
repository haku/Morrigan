package net.sparktank.nemain.shells;

import net.sparktank.nemain.model.NemainEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditEntryShell {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Shell parent;
	
	public EditEntryShell (Shell parent) {
		this.parent = parent;
		makeShell();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean exitValue;
	private String exitText;
	
	private final static int SEP = 3;
	
	private Shell shell;
	private Text text = null;
	private Button btnConfirm = null;
	private Button btnCancel = null;
	
	private void makeShell () {
		FormData formData;
		
		shell = new Shell(parent.getDisplay(), SWT.TITLE | SWT.CLOSE | SWT.PRIMARY_MODAL | SWT.RESIZE);
		text = new Text(shell, SWT.MULTI | SWT.BORDER);
		btnConfirm = new Button(shell, SWT.PUSH);
		btnCancel = new Button(shell, SWT.PUSH);
		
		shell.setImage(parent.getImage());
		shell.setText("Edit entry");
		shell.setLayout(new FormLayout());
		shell.setDefaultButton(btnConfirm);
		shell.addTraverseListener(traverseListener);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(btnConfirm, -SEP);
		formData.width = 500;
		formData.height = 300;
		text.setLayoutData(formData);
		
		btnConfirm.setText("Confirm");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnConfirm.setLayoutData(formData);
		
		btnCancel.setText("Cancel");
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnCancel.setLayoutData(formData);
		
		btnConfirm.addSelectionListener(buttonListener);
		btnCancel.addSelectionListener(buttonListener);
		
		shell.pack();
	}
	
	public void remoteClose () {
		leaveDlg(false);
	}
	
	private void leaveDlg (boolean ok) {
		exitValue = ok;
		
		if (ok) {
			exitText = text.getText();
		} else {
			exitText = null;
		}
		
		shell.close();
	}
	
	private TraverseListener traverseListener = new TraverseListener() {
		public void keyTraversed(TraverseEvent e) {
			switch (e.detail) {
				
//				case SWT.TRAVERSE_RETURN:
//					e.detail = SWT.TRAVERSE_NONE;
//					e.doit = false;
//					leaveDlg(true);
//					break;
					
				case SWT.TRAVERSE_ESCAPE:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(false);
					break;
				
				default:
					throw new IllegalArgumentException();
					
			}
		}
	};
	
	private SelectionListener buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == btnConfirm) {
				leaveDlg(true);
				
			} else {
				leaveDlg(false);
				
			}
		}
	};
	
	private void showAndWaitForShellToClose () {
		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean showDlg (NemainEvent event) {
		shell.setText("Entry for " + event.getDate().toString());
		text.setText(event.getEntryText());
		
		showAndWaitForShellToClose();
		return exitValue;
	}
	
	public String getExitText() {
		return exitText;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

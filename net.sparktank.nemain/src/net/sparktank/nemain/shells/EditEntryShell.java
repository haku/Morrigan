/*
 * Copyright 2010 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
	Button btnConfirm = null;
	private Button btnCancel = null;
	
	private void makeShell () {
		FormData formData;
		
		this.shell = new Shell(this.parent.getDisplay(), SWT.TITLE | SWT.CLOSE | SWT.PRIMARY_MODAL | SWT.RESIZE);
		this.text = new Text(this.shell, SWT.MULTI | SWT.WRAP | SWT.BORDER);
		this.btnConfirm = new Button(this.shell, SWT.PUSH);
		this.btnCancel = new Button(this.shell, SWT.PUSH);
		
		this.shell.setImage(this.parent.getImage());
		this.shell.setText("Edit entry");
		this.shell.setLayout(new FormLayout());
		this.shell.addTraverseListener(this.traverseListener);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(this.btnConfirm, -SEP);
		formData.width = 500;
		formData.height = 300;
		this.text.setLayoutData(formData);
		
		this.btnConfirm.setText("Confirm");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnConfirm.setLayoutData(formData);
		
		this.btnCancel.setText("Cancel");
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnCancel.setLayoutData(formData);
		
		this.btnConfirm.addSelectionListener(this.buttonListener);
		this.btnCancel.addSelectionListener(this.buttonListener);
		
		this.shell.pack();
	}
	
	public void remoteClose () {
		leaveDlg(false);
	}
	
	void leaveDlg (boolean ok) {
		this.exitValue = ok;
		
		if (ok) {
			this.exitText = this.text.getText();
		} else {
			this.exitText = null;
		}
		
		this.shell.close();
	}
	
	private TraverseListener traverseListener = new TraverseListener() {
		@Override
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
				
			}
		}
	};
	
	private SelectionListener buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == EditEntryShell.this.btnConfirm) {
				leaveDlg(true);
				
			} else {
				leaveDlg(false);
				
			}
		}
	};
	
	private void showAndWaitForShellToClose () {
		this.shell.open();
		Display display = this.parent.getDisplay();
		while (!this.shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean showDlg (NemainEvent event) {
		this.shell.setText("Entry for " + event.getDateAsString());
		this.text.setText(event.getEntryText());
		
		showAndWaitForShellToClose();
		return this.exitValue;
	}
	
	public String getExitText() {
		return this.exitText;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

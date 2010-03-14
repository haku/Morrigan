package net.sparktank.morrigan.dialogs;

import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.media.PlayItem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class JumpToDlg extends Dialog {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = -6047405753309652452L;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaLibrary mediaLibrary;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public JumpToDlg (Shell parent, MediaLibrary mediaLibrary) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		this.mediaLibrary = mediaLibrary;
		setText("Jump to track");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static int SEP = 3;
	
	private PlayItem returnValue = null;
	
	public PlayItem open () {
		FormData formData;
		
		final Shell shell = new Shell(getParent(), getStyle());
		shell.setLayout(new FormLayout());
		
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
		
		btnOk.addSelectionListener(listener);
		btnCancel.addSelectionListener(listener);
		
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
	
	private SelectionListener listener = new SelectionListener() {
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
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

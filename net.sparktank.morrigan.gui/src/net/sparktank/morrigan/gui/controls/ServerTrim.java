package net.sparktank.morrigan.gui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class ServerTrim extends WorkbenchWindowControlContribution {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.controls.ServerTrim";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Label lblStatus;
	private Button btnStartStop;
	
	@Override
	protected Control createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		this.lblStatus = new Label(composite, SWT.NONE);
		this.btnStartStop = new Button(composite, SWT.PUSH);
		
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		composite.setLayout(layout);
		
		this.lblStatus.setText("Server stopped.");
		this.lblStatus.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		
		this.btnStartStop.setText("Start");
		this.btnStartStop.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		
		return composite;
	}
	
	@Override
	public boolean isDynamic() {
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

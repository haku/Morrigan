package com.vaguehope.morrigan.gui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.helpers.TrayHelper;

public class MinToTrayAction  extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IWorkbenchWindow window;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MinToTrayAction (IWorkbenchWindow window) {
		super();
		this.window = window;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return "Minimise to tray"; }
	
	@Override
	public String getId() { return "mintotray"; }
	
	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run () {
		if (!TrayHelper.minToTray(this.window, true)) {
			new MorriganMsgDlg("No system tray available.").open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

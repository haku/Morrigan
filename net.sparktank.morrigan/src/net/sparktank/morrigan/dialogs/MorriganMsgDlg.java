package net.sparktank.morrigan.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class MorriganMsgDlg extends MessageDialog {

	public MorriganMsgDlg(String dialogMessage) {
		super(
				Display.getCurrent().getActiveShell(), 
				"Morrigan", null, 
				dialogMessage, 
				MessageDialog.INFORMATION, new String[] {"Continue"}, 0);
	}

}

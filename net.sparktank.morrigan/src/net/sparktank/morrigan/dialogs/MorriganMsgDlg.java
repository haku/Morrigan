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
	
	public MorriganMsgDlg(Exception e) {
		super(
				Display.getCurrent().getActiveShell(), 
				"Morrigan", null, 
				getErrMsg(e), 
				MessageDialog.ERROR, new String[] {"Continue"}, 0);
	}
	
	private static String getErrMsg (Exception e) {
		String m = "";
		Throwable c = e;
		while (true) {
			if (m.length()>0) m = m + "\n   caused by ";
			m = m + c.getClass().getName() + ": " + c.getMessage();
			c = c.getCause();
			if (c==null) break;
		}
		return m;
	}

}

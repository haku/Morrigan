package net.sparktank.morrigan.gui.dialogs;

import net.sparktank.morrigan.gui.helpers.ClipboardHelper;
import net.sparktank.morrigan.gui.helpers.ErrorHelper;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class MorriganMsgDlg extends MessageDialog {
	
	public static final String[] YESNO = {"Yes", "No"};
	public static final String[] COPYCONTINUE = {"Copy", "Continue"};
	
	private Throwable throwable = null;
	
	public MorriganMsgDlg(String dialogMessage) {
		super(
				Display.getCurrent().getActiveShell(), 
				"Morrigan", null, 
				dialogMessage, 
				MessageDialog.INFORMATION, new String[] {"Continue"}, 0);
	}
	
	public MorriganMsgDlg(String dialogMessage, String[] answers) {
		super(
				Display.getCurrent().getActiveShell(), 
				"Morrigan", null, 
				dialogMessage, 
				MessageDialog.INFORMATION, answers, 0);
	}
	
	public MorriganMsgDlg(Throwable t) {
		super(
				Display.getCurrent().getActiveShell(), 
				"Morrigan", null, 
				getErrMsg(t), 
				MessageDialog.ERROR, COPYCONTINUE, 0);
		
		throwable = t;
	}
	
	@Override
	public int open() {
		int open;
		try {
			open = super.open();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		
		if (throwable!=null && open==OK) {
			throwable.printStackTrace();
			ClipboardHelper.setText(ErrorHelper.getStackTrace(throwable), Display.getCurrent());
		}
		
		return open;
	}
	
	private static String getErrMsg (Throwable t) {
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		Throwable c = t;
		while (true) {
			if (!first) sb.append("\n   caused by ");
			first = false;
			sb.append(c.getClass().getName() + ": " + c.getMessage());
			c = c.getCause();
			if (c==null) break;
		}
		
		return sb.toString();
	}
	
}

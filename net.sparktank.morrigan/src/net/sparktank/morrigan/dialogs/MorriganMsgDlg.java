package net.sparktank.morrigan.dialogs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

public class MorriganMsgDlg extends MessageDialog {
	
	public static final String[] YESNO = {"Yes", "No"};
	public static final String[] COPYCONTINUE = {"Copy", "Continue"};
	
	private Exception exception = null;
	private Display display;
	
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
	
	public MorriganMsgDlg(Exception e, Display display) {
		super(
				Display.getCurrent().getActiveShell(), 
				"Morrigan", null, 
				getErrMsg(e), 
				MessageDialog.ERROR, COPYCONTINUE, 0);
		
		exception = e;
	}
	
	@Override
	public int open() {
		int open = super.open();
		
		if (exception!=null && open==OK) {
			Clipboard clipboard = new Clipboard(display);
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new String[]{getStackTrace(exception)}, new Transfer[]{textTransfer});
	        clipboard.dispose();
		}
		
		return open;
	}
	
	private static String getErrMsg (Exception e) {
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		Throwable c = e;
		while (true) {
			if (!first) sb.append("\n   caused by ");
			first = false;
			sb.append(c.getClass().getName() + ": " + c.getMessage());
			c = c.getCause();
			if (c==null) break;
		}
		
		return sb.toString();
	}
	
	public static String getStackTrace (Throwable t) {
		final Writer writer = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		return writer.toString();
	}
	
	
}

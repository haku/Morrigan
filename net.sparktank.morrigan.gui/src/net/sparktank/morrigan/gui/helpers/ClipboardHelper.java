package net.sparktank.morrigan.gui.helpers;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

public class ClipboardHelper {
	
	public static void setText (String text) {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		TextTransfer textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new String[]{text}, new Transfer[]{textTransfer});
        clipboard.dispose();
	}
	
}

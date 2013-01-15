package com.vaguehope.morrigan.gui.helpers;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

public final class ClipboardHelper {

	private ClipboardHelper () {}

	public static void setText (String text) {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		TextTransfer textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new String[] { text }, new Transfer[] { textTransfer });
		clipboard.dispose();
	}

}

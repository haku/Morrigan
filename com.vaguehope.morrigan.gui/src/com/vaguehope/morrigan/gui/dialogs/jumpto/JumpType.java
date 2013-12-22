package com.vaguehope.morrigan.gui.dialogs.jumpto;

import org.eclipse.swt.SWT;

public enum JumpType {

	NULL(null),
	PLAY_NOW("Enter"),
	ENQUEUE("C-Enter"),
	REVEAL("C-S-Enter"),
	SHUFFLE_AND_ENQUEUE("A-Enter"),
	OPEN_VIEW("C-S-A-Enter");

	private final String keyDes;

	private JumpType (final String keyDes) {
		this.keyDes = keyDes;
	}

	public String getKeyDes () {
		return this.keyDes;
	}

	public static JumpType fromDlg (final JumpToDlg dlg) {
		if (dlg.getReturnItem() == null) return NULL;
		if ((dlg.getKeyMask() & SWT.ALT) != 0 && dlg.getReturnList() != null) {
			if ((dlg.getKeyMask() & SWT.SHIFT) != 0 && (dlg.getKeyMask() & SWT.CONTROL) != 0) {
				return OPEN_VIEW;
			}
			return SHUFFLE_AND_ENQUEUE;
		}
		else if ((dlg.getKeyMask() & SWT.SHIFT) != 0 && (dlg.getKeyMask() & SWT.CONTROL) != 0) {
			return REVEAL;
		}
		else if ((dlg.getKeyMask() & SWT.SHIFT) != 0 || (dlg.getKeyMask() & SWT.CONTROL) != 0) {
			return ENQUEUE;
		}
		else {
			return PLAY_NOW;
		}
	}

}

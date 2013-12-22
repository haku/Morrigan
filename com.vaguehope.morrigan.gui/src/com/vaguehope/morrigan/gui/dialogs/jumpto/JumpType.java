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

	public static JumpType fromStateMask (final int stateMask) {
		final boolean ctrl = (stateMask & SWT.CONTROL) != 0;
		final boolean alt = (stateMask & SWT.ALT) != 0;
		final boolean shift = (stateMask & SWT.SHIFT) != 0;
		if (alt) {
			if (shift && ctrl) return OPEN_VIEW;
			return SHUFFLE_AND_ENQUEUE;
		}
		else if (shift && ctrl) {
			return REVEAL;
		}
		else if (shift || ctrl) {
			return ENQUEUE;
		}
		else {
			return PLAY_NOW;
		}
	}

}

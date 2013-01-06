package com.vaguehope.morrigan.screen;

import org.eclipse.swt.widgets.Composite;

public interface ScreenMgrCallback {

	void handleError (Exception e);

	/**
	 * Get the screen for when not in FullSreen mode.
	 */
	Composite getCurrentScreen ();

	void updateCurrentMediaFrameParent (Composite parent);

}

package com.vaguehope.morrigan.screen;

import org.eclipse.swt.widgets.Composite;

public interface ScreenMgrCallback {

	void handleError (Exception e);

	void updateCurrentMediaFrameParent (Composite parent);

}

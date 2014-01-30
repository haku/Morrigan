package com.vaguehope.morrigan.player;

import org.eclipse.swt.widgets.Composite;

public interface LocalPlayer extends Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void setVideoFrameParent(Composite cmfp);

	boolean isProxy();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
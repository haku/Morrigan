package com.vaguehope.morrigan.player;

import org.eclipse.swt.widgets.Composite;

public interface LocalPlayer extends Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void addQueueChangeListener(Runnable listener);
	void removeQueueChangeListener(Runnable listener);

	void setVideoFrameParent(Composite cmfp);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
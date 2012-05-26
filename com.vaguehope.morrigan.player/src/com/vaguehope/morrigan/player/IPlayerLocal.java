package com.vaguehope.morrigan.player;

import org.eclipse.swt.widgets.Composite;

/**
 * TODO rename to LocalPlayer
 */
public interface IPlayerLocal extends Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void addQueueChangeListener(Runnable listener);
	void removeQueueChangeListener(Runnable listener);

	void setVideoFrameParent(Composite cmfp);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
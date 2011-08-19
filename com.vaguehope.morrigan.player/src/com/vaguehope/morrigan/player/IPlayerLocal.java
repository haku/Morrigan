package com.vaguehope.morrigan.player;

import org.eclipse.swt.widgets.Composite;

public interface IPlayerLocal extends IPlayerAbstract {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void addQueueChangeListener(Runnable listener);
	public void removeQueueChangeListener(Runnable listener);
	
	public void setVideoFrameParent(Composite cmfp);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
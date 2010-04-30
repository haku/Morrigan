package net.sparktank.morrigan.player;

import org.eclipse.swt.widgets.Composite;

import net.sparktank.morrigan.model.MediaList;

public interface IPlayerEventHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean doOnForegroudThread (Runnable r) ;
		
	/**
	 * Called when:
	 * - position changed.
	 * - track changed.
	 */
	public void updateStatus () ;
	
	public void videoAreaSelected () ;
	
	public void videoAreaClose () ;
	
	public void historyChanged () ;
	
	public void currentItemChanged () ;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void asyncThrowable (Throwable t) ;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaList getCurrentList () ;
	
	public Composite getCurrentMediaFrameParent () ;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

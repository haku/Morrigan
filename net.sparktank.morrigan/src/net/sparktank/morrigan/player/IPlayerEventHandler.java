package net.sparktank.morrigan.player;

import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackList;

import org.eclipse.swt.widgets.Composite;

public interface IPlayerEventHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
	public IMediaTrackList<IMediaTrack> getCurrentList () ;
	
	public Composite getCurrentMediaFrameParent () ;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package com.vaguehope.morrigan.player;

import java.util.Map;

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
	
	public Map<Integer, String> getMonitors ();
	
	public void goFullscreen (int monitor);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

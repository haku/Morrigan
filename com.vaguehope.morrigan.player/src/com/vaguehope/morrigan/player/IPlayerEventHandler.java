package com.vaguehope.morrigan.player;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;

/**
 * rename to PlayerEventHandler
 */
public interface IPlayerEventHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Called when:
	 * - position changed.
	 * - track changed.
	 */
	void updateStatus () ;

	void videoAreaSelected () ;

	void videoAreaClose () ;

	void historyChanged () ;

	void currentItemChanged () ;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void asyncThrowable (Throwable t) ;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	IMediaTrackList<IMediaTrack> getCurrentList () ;

	Composite getCurrentMediaFrameParent () ;

	Map<Integer, String> getMonitors ();

	void goFullscreen (int monitor);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

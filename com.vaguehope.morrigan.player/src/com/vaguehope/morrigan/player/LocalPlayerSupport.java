package com.vaguehope.morrigan.player;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;

public interface LocalPlayerSupport {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * User double-clicked on the video?
	 */
	void videoAreaSelected () ;

	/**
	 * User pressed escape?
	 */
	void videoAreaClose () ;

	void historyChanged () ;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	IMediaTrackList<IMediaTrack> getCurrentList () ;

	Composite getCurrentMediaFrameParent () ;

	Map<Integer, String> getMonitors ();

	void goFullscreen (int monitor);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

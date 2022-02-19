package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;

public interface LocalPlayerSupport {

	void historyChanged();

	IMediaTrackList<IMediaTrack> getCurrentList();

}

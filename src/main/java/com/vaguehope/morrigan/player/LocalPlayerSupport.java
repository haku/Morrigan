package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.model.media.MediaList;

public interface LocalPlayerSupport {

	void historyChanged();

	MediaList getCurrentList();

}

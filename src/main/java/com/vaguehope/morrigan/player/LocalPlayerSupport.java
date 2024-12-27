package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.model.media.IMediaItemList;

public interface LocalPlayerSupport {

	void historyChanged();

	IMediaItemList getCurrentList();

}

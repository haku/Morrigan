package com.vaguehope.morrigan.android.playback;

public interface PlayerFragmentEventListener {

	void queueChanged ();
	void itemChanged (QueueItem item);

}

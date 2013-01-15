package com.vaguehope.morrigan.playbackimpl.gs;

public interface Constants {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constants.

	// TODO Any more?
	String[] SUPPORTED_FORMATS = {"wav", "mp3"};

	String[] AUDIO_ONLY_FORMATS = {"mp3", "ogg", "wav", "wma", "m4a", "aac", "ra", "mpc", "ac3"};

	// Timing properties.
	int FILE_READ_DURATION_TIMEOUT = 5000; // 5 seconds.

	int WAIT_FOR_decodeElement_POLL_INTERVAL = 200;
	int WAIT_FOR_decodeElement_TIMEOUT = 30000; // 30 seconds.
	int WAIT_FOR_PADS_POLL_INTERVAL = 200;
	int WAIT_FOR_PADS_TIMEOUT = 30000; // 30 seconds.

	int WATCHER_POLL_INTERVAL_MILLIS = 500; // 0.5 seconds.
	int EOS_MAN_LIMIT = 10; // 10*500 = 5 seconds.
	int EOS_MARGIN_SECONDS = 1; // 1 second from end of track.

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

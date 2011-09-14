package com.vaguehope.morrigan.playbackimpl.gs;

public class Constants {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constants.
	
	// TODO Any more?
	public final static String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
	public final static String[] AUDIO_ONLY_FORMATS = {"mp3", "ogg", "wav", "wma", "m4a", "aac", "ra", "mpc", "ac3"};
	
	// Timing properties.
	public static final int FILE_READ_DURATION_TIMEOUT = 5000; // 5 seconds.
	
	public static final int WAIT_FOR_decodeElement_POLL_INTERVAL = 200;
	public static final int WAIT_FOR_decodeElement_TIMEOUT = 30000; // 30 seconds.
	public static final int WAIT_FOR_PADS_POLL_INTERVAL = 200;
	public static final int WAIT_FOR_PADS_TIMEOUT = 30000; // 30 seconds.
	
	public static final int WATCHER_POLL_INTERVAL_MILLIS = 500; // 0.5 seconds.
	public static final int EOS_MAN_LIMIT = 10; // 10*500 = 5 seconds.
	public static final int EOS_MARGIN_SECONDS = 1; // 1 second from end of track.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

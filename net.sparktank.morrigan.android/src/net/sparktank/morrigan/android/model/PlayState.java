package net.sparktank.morrigan.android.model;

/**
 * partly copy-pasted from net.sparktank.morrigan.engines.playback.IPlaybackEngine.
 * TODO avoid copy-paste.
 */
public enum PlayState {
	
	STOPPED(0), PLAYING(1), PAUSED(2), LOADING(3);
	
	private int n;
	
	private PlayState (int n) {
		this.n = n;
	}
	
	public int getN() {
		return this.n;
	}
	
	static public PlayState parseN (int number) {
		switch (number) {
			case 0: return STOPPED;
			case 1: return PLAYING;
			case 2: return PAUSED;
			case 3: return LOADING;
			default: throw new IllegalArgumentException();
		}
	}
	
}

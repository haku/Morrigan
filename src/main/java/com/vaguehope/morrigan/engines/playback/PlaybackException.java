package com.vaguehope.morrigan.engines.playback;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

public class PlaybackException extends MorriganException {

	private static final long serialVersionUID = 5570985525593129951L;

	public PlaybackException (String s) {
		super(s);
	}

	public PlaybackException (String s, Throwable t) {
		super(s, t);
	}

	public PlaybackException(Throwable t) {
		super(t);
	}

}

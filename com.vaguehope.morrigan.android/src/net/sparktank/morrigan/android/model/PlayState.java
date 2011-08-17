/*
 * Copyright 2010 Fae Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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

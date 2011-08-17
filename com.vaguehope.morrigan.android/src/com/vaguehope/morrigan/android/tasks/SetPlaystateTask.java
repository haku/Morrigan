/*
 * Copyright 2010 Alex Hutter
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

package com.vaguehope.morrigan.android.tasks;

import java.io.IOException;
import java.io.InputStream;


import org.xml.sax.SAXException;

import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.PlayerState;
import com.vaguehope.morrigan.android.model.PlayerStateChangeListener;
import com.vaguehope.morrigan.android.model.impl.PlayerStateXmlImpl;

import android.app.Activity;

public class SetPlaystateTask extends AbstractTask<PlayerState> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum TargetPlayState {
		STOP(0), NEXT(1), PLAYPAUSE(2);
		
		private int n;
		
		private TargetPlayState (int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final PlayerReference playerReference;
	private final PlayerStateChangeListener changeListener;
	
	private final TargetPlayState targetPlayState;
	private final int fullscreenMonitor;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public SetPlaystateTask (Activity activity, PlayerReference playerReference, PlayerStateChangeListener changeListener) {
		this(activity, playerReference, null, changeListener);
	}
	
	public SetPlaystateTask (Activity activity, PlayerReference playerReference, TargetPlayState targetPlayState, PlayerStateChangeListener changeListener) {
		super(activity);
		this.playerReference = playerReference;
		this.targetPlayState = targetPlayState;
		this.fullscreenMonitor = -1;
		this.changeListener = changeListener;
	}
	
	public SetPlaystateTask (Activity activity, PlayerReference playerReference, int fullscreenMonitor, PlayerStateChangeListener changeListener) {
		super(activity);
		this.playerReference = playerReference;
		this.targetPlayState = null;
		this.fullscreenMonitor = fullscreenMonitor;
		this.changeListener = changeListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String verb = null;
	private String encodedData = null;
	
	@Override
	protected String getUrl () {
		String url = this.playerReference.getBaseUrl();
		
		if (this.targetPlayState != null) {
			this.verb = "POST";
			this.encodedData = "action=";
			switch (this.targetPlayState) {
				case PLAYPAUSE:
					this.encodedData = this.encodedData.concat("playpause");
					break;
					
				case NEXT:
					this.encodedData = this.encodedData.concat("next");
					break;
					
				case STOP:
					this.encodedData = this.encodedData.concat("stop");
					break;
					
				default: throw new IllegalArgumentException();
			}
		}
		else if (this.fullscreenMonitor >= 0) {
			this.verb = "POST";
			this.encodedData = "action=fullscreen&monitor=" + this.fullscreenMonitor;
		}
		
		return url;
	}
	
	@Override
	protected String getVerb () {
		return this.verb; // TODO always POST ?
	}
	
	@Override
	protected String getEncodedData () {
		return this.encodedData;
	}
	
	@Override
	protected String getContentType () {
		return "application/x-www-form-urlencoded";
	}
	
	// In background thread:
	@Override
	protected PlayerState parseStream (InputStream is) throws IOException, SAXException {
		return new PlayerStateXmlImpl(is, this.playerReference);
	}
	
	// In UI thread:
	@Override
	protected void onSuccess (PlayerState result) {
		if (this.changeListener != null) this.changeListener.onPlayerStateChange(result);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

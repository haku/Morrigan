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

package net.sparktank.morrigan.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.helper.HttpHelper.HttpStreamHandler;
import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.PlayerStateChangeListener;
import net.sparktank.morrigan.android.model.impl.PlayerStateXmlImpl;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

public class SetPlaystateTask extends AsyncTask<Void, Void, PlayerState> {
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
	
	private final Activity activity;
	protected final PlayerReference playerReference;
	private final PlayerStateChangeListener changeListener;
	
	private final TargetPlayState targetPlayState;
	private final int fullscreenMonitor;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public SetPlaystateTask (Activity activity, PlayerReference playerReference, PlayerStateChangeListener changeListener) {
		this(activity, playerReference, null, changeListener);
	}
	
	public SetPlaystateTask (Activity activity, PlayerReference playerReference, TargetPlayState targetPlayState, PlayerStateChangeListener changeListener) {
		this.activity = activity;
		this.playerReference = playerReference;
		this.targetPlayState = targetPlayState;
		this.fullscreenMonitor = -1;
		this.changeListener = changeListener;
	}
	
	public SetPlaystateTask (Activity activity, PlayerReference playerReference, int fullscreenMonitor, PlayerStateChangeListener changeListener) {
		this.activity = activity;
		this.playerReference = playerReference;
		this.targetPlayState = null;
		this.fullscreenMonitor = fullscreenMonitor;
		this.changeListener = changeListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.activity.setProgressBarIndeterminateVisibility(true);
	}
	
	// In background thread:
	@Override
	protected PlayerState doInBackground(Void... params) {
		String url = this.playerReference.getBaseUrl();
		
		String verb = null;
		String encodedData = null;
		
		if (this.targetPlayState != null) {
			verb = "POST";
			encodedData = "action=";
    		switch (this.targetPlayState) {
    			case PLAYPAUSE:
    				encodedData = encodedData.concat("playpause");
    				break;
    				
    			case NEXT:
    				encodedData = encodedData.concat("next");
    				break;
    				
    			case STOP:
    				encodedData = encodedData.concat("stop");
    				break;
    				
    			default: throw new IllegalArgumentException();
    		}
		}
		else if (this.fullscreenMonitor >= 0) {
			verb = "POST";
			encodedData = "action=fullscreen&monitor=" + this.fullscreenMonitor;
		}
		
		try {
			final AtomicReference<PlayerState> state = new AtomicReference<PlayerState>();
			
			HttpStreamHandler<SAXException> handler = new HttpStreamHandler<SAXException>() {
				@Override
				public void handleStream(InputStream is) throws IOException, SAXException {
					PlayerState playerState = new PlayerStateXmlImpl(is, SetPlaystateTask.this.playerReference);
					state.set(playerState);
				}
			};
			
			HttpHelper.getUrlContent(url, verb, encodedData, "application/x-www-form-urlencoded", handler);
			
			return state.get();
		}
		catch (ConnectException e) {
			this.exception = e;
			return null;
		} catch (IOException e) {
			this.exception = e;
			return null;
		} catch (SAXException e) {
			this.exception = e;
			return null;
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(PlayerState result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_SHORT).show();
		}
		
		if (this.changeListener != null) this.changeListener.onPlayerStateChange(result);
		
		this.activity.setProgressBarIndeterminateVisibility(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

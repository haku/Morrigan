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

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.PlayerStateChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.PlayerStateImpl;

import org.xml.sax.SAXException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

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
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final ServerReference serverReference;
	private final TargetPlayState targetPlayState;
	private final PlayerStateChangeListener changeListener;
	
	private ProgressDialog dialog;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public SetPlaystateTask (Context context, ServerReference serverReference, TargetPlayState targetPlayState, PlayerStateChangeListener changeListener) {
		this.context = context;
		this.serverReference = serverReference;
		this.targetPlayState = targetPlayState;
		this.changeListener = changeListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog = ProgressDialog.show(this.context, "Play / Pause", "Please wait...", true);
	}
	
	// In background thread:
	@Override
	protected PlayerState doInBackground(Void... params) {
		String url = this.serverReference.getBaseUrl();
		url = url.concat("/players/0"); // TODO remove temp hard-coded values.
		
		String encodedData = "action=";
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
				
			default:
				throw new IllegalArgumentException();
			
		}
		
		try {
			// TODO parse response?
			String resp = HttpHelper.getUrlContent(url, "POST", encodedData, "application/x-www-form-urlencoded");
			PlayerState playerState = new PlayerStateImpl(resp);
			return playerState;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(PlayerState result) {
		super.onPostExecute(result);
		
		if (this.changeListener != null) this.changeListener.onPlayerStateChange(result);
		
		this.dialog.dismiss(); // FIXME This will fail if the screen is rotated while we are fetching.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

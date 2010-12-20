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

package net.sparktank.morrigan.android.tasks;

import java.io.IOException;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.ServerReference;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class SetPlaystateTask extends AsyncTask<Void, Void, Boolean> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum TargetPlayState {
		STOPPED(0), PLAY(1), NEXT(2), PLAYPAUSE(3);
		
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
	
	private ProgressDialog dialog;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public SetPlaystateTask (Context context, ServerReference serverReference, TargetPlayState targetPlayState) {
		this.context = context;
		this.serverReference = serverReference;
		this.targetPlayState = targetPlayState;
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
	protected Boolean doInBackground(Void... params) {
		String url = this.serverReference.getBaseUrl();
		url = url.concat("/player/0"); // TODO remove temp hard-coded values.
		
		switch (this.targetPlayState) {
			case PLAYPAUSE:
				url = url.concat("/playpause");
				break;
				
			case NEXT:
				url = url.concat("/next");
				break;
				
			case PLAY:
				url = url.concat("/play");
				break;
				
			default:
				throw new IllegalArgumentException();
			
		}
		
		try {
			// TODO parse response?
			HttpHelper.getUrlContent(url);
			
		}
		catch (IOException e) {
			e.printStackTrace();
			return Boolean.FALSE;
		}
		
		return Boolean.TRUE;
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		
		Toast.makeText(this.context, this.targetPlayState.toString() + " result: " + result, Toast.LENGTH_LONG).show();
		
		// FIXME This will fail if the screen is rotated while we are fetching.
		this.dialog.dismiss();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

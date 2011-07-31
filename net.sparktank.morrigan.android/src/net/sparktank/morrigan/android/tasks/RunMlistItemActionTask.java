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
import java.net.ConnectException;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.PlayerReference;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RunMlistItemActionTask extends AsyncTask<Void, Void, String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public enum MlistItemCommand {
		PLAY(0), QUEUE(1);
		
		private int n;
		
		private MlistItemCommand (int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	private final PlayerReference playerReference;
	private final MlistItem mlistItem;
	private final MlistItemCommand cmd;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RunMlistItemActionTask (Activity activity, PlayerReference playerReference, MlistItem mlistItem, MlistItemCommand cmd) {
		this.activity = activity;
		this.playerReference = playerReference;
		this.mlistItem = mlistItem;
		this.cmd = cmd;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ProgressDialog dialog;
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog = ProgressDialog.show(this.activity, null, "Please wait...", true);
	}
	
	// In background thread:
	@Override
	protected String doInBackground(Void... params) {
		String url = this.playerReference.getServerReference().getBaseUrl() + this.mlistItem.getRelativeUrl();
		
		String encodedData = "action=";
		switch (this.cmd) {
			case PLAY:
				encodedData = encodedData.concat("play");
				break;
				
			case QUEUE:
				encodedData = encodedData.concat("queue");
				break;
				
			default: throw new IllegalArgumentException();
		}
		
		encodedData = encodedData.concat("&playerid=" + String.valueOf(this.playerReference.getPlayerId()));
		
		try {
			String resp = HttpHelper.getUrlContent(url, "POST", encodedData, "application/x-www-form-urlencoded");
			return resp;
		}
		catch (ConnectException e) {
			this.exception = e;
			return null;
		} catch (IOException e) {
			this.exception = e;
			return null;
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e("Morrigan", "result=" + result, this.exception);
		}
		else {
			Toast.makeText(this.activity, result, Toast.LENGTH_SHORT).show();
		}
		
		this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

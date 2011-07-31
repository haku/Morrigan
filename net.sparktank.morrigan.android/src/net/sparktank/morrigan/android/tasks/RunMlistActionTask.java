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
import java.net.ConnectException;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.PlayerReference;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

public class RunMlistActionTask extends AsyncTask<Void, Void, String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public enum MlistCommand {
		PLAY(0), QUEUE(1), SCAN(2);
		
		private int n;
		
		private MlistCommand (int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	private final MlistReference mlistReference;
	private final MlistCommand cmd;
	private final PlayerReference playerReference;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RunMlistActionTask (Activity activity, MlistReference mlistReference, MlistCommand cmd) {
		this(activity, mlistReference, cmd, null);
	}
	
	public RunMlistActionTask (Activity activity, MlistReference mlistReference, MlistCommand cmd, PlayerReference playerReference) {
		if ((cmd == MlistCommand.PLAY || cmd == MlistCommand.QUEUE) && playerReference == null) throw new IllegalArgumentException("No player specified.");
		
		this.activity = activity;
		this.mlistReference = mlistReference;
		this.cmd = cmd;
		this.playerReference = playerReference;
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
		String encodedData = "action=";
		switch (this.cmd) {
			case PLAY:
				encodedData = encodedData.concat("play").concat("&playerid=" + this.playerReference.getPlayerId());
				break;
			
			case QUEUE:
				encodedData = encodedData.concat("queue").concat("&playerid=" + this.playerReference.getPlayerId());
				break;
			
			case SCAN:
				encodedData = encodedData.concat("scan");
				break;
			
			default: throw new IllegalArgumentException();
		}
		
		try {
			String resp = HttpHelper.getUrlContent(this.mlistReference.getBaseUrl(), "POST", encodedData, "application/x-www-form-urlencoded");
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
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this.activity, result, Toast.LENGTH_LONG).show();
		}
		
		this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

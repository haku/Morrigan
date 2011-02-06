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
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.android.Constants;
import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.helper.HttpHelper.HttpStreamHandler;
import net.sparktank.morrigan.android.model.PlayerStateList;
import net.sparktank.morrigan.android.model.PlayerStateListChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.PlayerStateListImpl;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class GetPlayersTask extends AsyncTask<Void, Void, PlayerStateList> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final Activity activity;
	protected final ServerReference serverReference;
	private final PlayerStateListChangeListener changedListener;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetPlayersTask (Context context, ServerReference serverReference, PlayerStateListChangeListener changedListener) {
		this.context = context;
		this.activity = null;
		this.serverReference = serverReference;
		this.changedListener = changedListener;
	}
	
	public GetPlayersTask (Activity activity, ServerReference serverReference, PlayerStateListChangeListener changedListener) {
		this.context = activity;
		this.activity = activity;
		this.serverReference = serverReference;
		this.changedListener = changedListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ProgressDialog dialog;
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (this.activity != null) {
			this.activity.setProgressBarIndeterminateVisibility(true);
		}
		else {
			this.dialog = ProgressDialog.show(this.context, null, "Please wait...", true);
		}
	}
	
	// In background thread:
	@Override
	protected PlayerStateList doInBackground(Void... params) {
		try {
			return fetchPlayerList(this.serverReference);
		}
		catch (IOException e) {
			this.exception = e;
			return null;
		} catch (SAXException e) {
			this.exception = e;
			return null;
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(PlayerStateList result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.context, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		if (this.changedListener != null) this.changedListener.onPlayersChange(result);
		
		if (this.dialog != null) this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
		if (this.activity != null) this.activity.setProgressBarIndeterminateVisibility(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public PlayerStateList fetchPlayerList (final ServerReference serverReference) throws IOException, SAXException {
		String url = serverReference.getBaseUrl();
		url = url.concat(Constants.CONTEXT_PLAYERS);
		
		final AtomicReference<PlayerStateList> list = new AtomicReference<PlayerStateList>();
		
		HttpStreamHandler<SAXException> handler = new HttpStreamHandler<SAXException>() {
			@Override
			public void handleStream(InputStream is) throws IOException, SAXException {
				PlayerStateList l;
				l = new PlayerStateListImpl(is, serverReference);
				list.set(l);
			}
		};
		
		HttpHelper.getUrlContent(url, handler);
		
		return list.get();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

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

import net.sparktank.morrigan.android.Constants;
import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.PlayersChangedListener;
import net.sparktank.morrigan.android.model.PlayersState;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.PlayersStateImpl;

import org.xml.sax.SAXException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class GetPlayersTask extends AsyncTask<Void, Void, PlayersState> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final ServerReference serverReference;
	private final PlayersChangedListener changedListener;
	
	private ProgressDialog dialog;
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetPlayersTask (Context context, ServerReference serverReference, PlayersChangedListener changedListener) {
		this.context = context;
		this.serverReference = serverReference;
		this.changedListener = changedListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog = ProgressDialog.show(this.context, null, "Please wait...", true);
	}
	
	// In background thread:
	@Override
	protected PlayersState doInBackground(Void... params) {
		String url = this.serverReference.getBaseUrl();
		url = url.concat(Constants.CONTEXT_PLAYERS);
		
		try {
			String resp = HttpHelper.getUrlContent(url);
			PlayersState playersState = new PlayersStateImpl(resp);
			return playersState;
		}
		catch (ConnectException e) {
			this.exception = e;
			return null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(PlayersState result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.context, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		if (this.changedListener != null) this.changedListener.onPlayersChange(result);
		
		this.dialog.dismiss(); // FIXME This will fail if the screen is rotated while we are fetching.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

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
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.android.Constants;
import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.helper.HttpHelper.HttpStreamHandler;
import net.sparktank.morrigan.android.model.PlayerQueue;
import net.sparktank.morrigan.android.model.PlayerQueueChangeListener;
import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.impl.PlayerQueueImpl;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

public class GetPlayerQueueTask extends AsyncTask<Void, Void, PlayerQueue> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	protected final PlayerReference playerReference;
	private final PlayerQueueChangeListener changeListener;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetPlayerQueueTask (Activity activity, PlayerReference playerReference, PlayerQueueChangeListener changeListener) {
		this.activity = activity;
		this.playerReference = playerReference;
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
	protected PlayerQueue doInBackground(Void... params) {
		String url = this.playerReference.getBaseUrl();
		url = url.concat(Constants.CONTEXT_PLAYER_QUEUE);
		
		try {
			final AtomicReference<PlayerQueue> queue = new AtomicReference<PlayerQueue>();
			
			HttpStreamHandler<SAXException> handler = new HttpStreamHandler<SAXException>() {
				@Override
				public void handleStream(InputStream is) throws IOException, SAXException {
					PlayerQueue q;
					q = new PlayerQueueImpl(is, GetPlayerQueueTask.this.playerReference);
					queue.set(q);
				}
			};
			
			HttpHelper.getUrlContent(url, handler);
			
			return queue.get();
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
	protected void onPostExecute(PlayerQueue result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		if (this.changeListener != null) this.changeListener.onPlayerQueueChange(result);
		
		this.activity.setProgressBarIndeterminateVisibility(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

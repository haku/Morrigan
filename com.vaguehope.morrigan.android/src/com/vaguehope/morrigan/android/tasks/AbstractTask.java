/*
 * Copyright 2011 Alex Hutter
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
import java.util.concurrent.atomic.AtomicReference;


import org.xml.sax.SAXException;

import com.vaguehope.morrigan.android.Constants;
import com.vaguehope.morrigan.android.helper.HttpHelper;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpStreamHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public abstract class AbstractTask<T extends Object> extends AsyncTask<Void, Void, T> implements HttpStreamHandler<SAXException> {
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	private final Context context;
	private Exception exception;
	
	private ProgressDialog dialog;
	private final AtomicReference<T> state = new AtomicReference<T>();
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public AbstractTask (Activity activity) {
		this.activity = activity;
		this.context = activity;
	}
	
	public AbstractTask (Context context) {
		this.activity = null;
		this.context = context;
	}
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// In UI thread:
	@Override
	final protected void onPreExecute () {
		super.onPreExecute();
		
		String prgMsg = getProgressMsg();
		if (prgMsg != null && this.context != null) {
			this.dialog = ProgressDialog.show(this.context, null, prgMsg, true);
		}
		else if (this.activity != null) {
			this.activity.setProgressBarIndeterminateVisibility(true); // TODO count up.
		}
		else if (this.context != null) {
			this.dialog = ProgressDialog.show(this.context, null, "Out of cheese...", true);
		}
		else {
			throw new IllegalStateException("Both activity and context are null.");
		}
	}
	
	@Override
	public void handleStream (InputStream is, int contentLength) throws IOException, SAXException {
		this.state.set(this.parseStream(is));
	}
	
	// In background thread:
	@Override
	final protected T doInBackground (Void... params) {
		try {
			// The order these are called in is important.
			String url = getUrl();
			String verb = getVerb();
			String encodedData = getEncodedData();
			String contentType = getContentType();
			HttpHelper.getUrlContent(url, verb, encodedData, contentType, this, getCreds());
			return this.state.get();
		}
		catch (Exception e) {
			this.exception = e;
			return null;
		}
	}
	
	// In UI thread:
	@Override
	final protected void onPostExecute (T result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.context, this.exception.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e(Constants.LOGTAG, "Task throw exception.", this.exception);
		}
		
		onSuccess(result);
		
		if (this.dialog != null) this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
		if (this.activity != null) this.activity.setProgressBarIndeterminateVisibility(false);
	}
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Activity getActivity () {
		return this.activity;
	}
	
	public Context getContext () {
		return this.context;
	}
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected String getProgressMsg () { return null; }
	protected abstract HttpCreds getCreds ();
	protected abstract String getUrl ();
	protected String getVerb () { return "GET"; }
	protected String getEncodedData () { return null; }
	protected String getContentType () { return null; }
	
	/**
	 * In background thread.
	 */
	protected abstract T parseStream (InputStream is) throws IOException, SAXException;
	
	/**
	 * In UI thread.
	 */
	protected abstract void onSuccess (T result);
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String parseStreamToString (InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		HttpHelper.buildString(is, sb);
		return sb.toString();
	}
	
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

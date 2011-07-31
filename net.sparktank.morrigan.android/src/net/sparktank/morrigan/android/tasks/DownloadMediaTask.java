/*
 * Copyright 2011 Fae Hutter
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

import java.io.File;
import java.io.IOException;

import net.sparktank.morrigan.android.helper.HttpFileDownloadHandler;
import net.sparktank.morrigan.android.helper.HttpFileDownloadHandler.DownloadProgressListener;
import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.ServerReference;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class DownloadMediaTask extends AsyncTask<MlistItem, Integer, String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final ServerReference serverReference;

	private ProgressDialog dialog;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public DownloadMediaTask (Context context, ServerReference serverReference) {
		this.context = context;
		this.serverReference = serverReference;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Context getContext () {
		return this.context;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static int PRGMAX = 10000;
	
	@Override
	protected void onPreExecute () {
		ProgressDialog progressDialog = new ProgressDialog(this.context);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(false);
		progressDialog.setMax(PRGMAX);
		progressDialog.setTitle("Downloading...");
//		progressDialog.setProgressNumberFormat(null); // Not available 'till API 11 (3.x).
		progressDialog.show();
		this.dialog = progressDialog;
	}
	
	@Override
	protected String doInBackground (MlistItem... items) {
		if (items.length < 1) throw new IllegalArgumentException("No items desu~");
		
		final int pPerItem = (int) (PRGMAX / (float)items.length);
		DownloadProgressListener progressListener = new DownloadProgressListener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void downloadProgress (int bytesRead, int totalBytes) {
				float p = (bytesRead / (float)totalBytes) * pPerItem;
				publishProgress(Integer.valueOf(0), Integer.valueOf((int) p));
			}
		};
		
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File (sdCard.getAbsolutePath() + "/morrigan"); // TODO make this configurable.
		dir.mkdirs();
		
		for (MlistItem item : items) {
			final String url = this.serverReference.getBaseUrl() + item.getRelativeUrl();
			final File file = new File(dir, item.getFileName());
			
			try {
				HttpHelper.getUrlContent(url, new HttpFileDownloadHandler(file, progressListener));
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			publishProgress(Integer.valueOf(pPerItem));
		}
		
		return "Download complete desu~";
	}
	
	@Override
	protected void onProgressUpdate (Integer... values) {
		// Note that one is increment and one is set.
		if (values.length >= 1 && values[0] != null && values[0].intValue() > 0) {
			this.dialog.incrementProgressBy(values[0].intValue());
		}
		if (values.length >= 2 && values[1] != null && values[1].intValue() > 0) {
			this.dialog.setSecondaryProgress(values[1].intValue());
		}
	}
	
	@Override
	protected void onPostExecute (String result) {
		if (this.dialog != null) this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
		Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

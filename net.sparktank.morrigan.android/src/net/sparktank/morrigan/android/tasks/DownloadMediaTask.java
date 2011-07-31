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

package net.sparktank.morrigan.android.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.helper.HttpHelper.HttpStreamHandler;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.ServerReference;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class DownloadMediaTask extends AsyncTask<MlistItem, Integer, String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int DOWNLOADBUFFERSIZE = 8192;
	private static final int GUIUPDATEINTERVALMILLIS = 500;
	
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
//	- - - - - - - - - - - - - - - - - - - -
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
		
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File (sdCard.getAbsolutePath() + "/morrigan");
		dir.mkdirs();
		
		for (MlistItem item : items) {
			final String url = this.serverReference.getBaseUrl() + item.getRelativeUrl();
			final File file = new File(dir, item.getFileName());
			
			try {
				HttpHelper.getUrlContent(url, new HttpStreamHandler<RuntimeException>() {
					@SuppressWarnings("synthetic-access")
					@Override
					public void handleStream (InputStream is, int contentLength) throws IOException {
						BufferedInputStream bis = new BufferedInputStream(is);
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
						try { // TODO this could probably be done better.
							byte[] buffer = new byte[DOWNLOADBUFFERSIZE];
							int bytesRead;
							int totalBytesRead = 0;
							long lastPublishTime = 0;
							while ((bytesRead = bis.read(buffer)) != -1) {
								bos.write(buffer, 0, bytesRead);
								totalBytesRead = totalBytesRead + bytesRead;
								
								if (contentLength > 0) {
									if (System.currentTimeMillis() - lastPublishTime > GUIUPDATEINTERVALMILLIS) {
										float p = (totalBytesRead / (float)contentLength) * pPerItem;
										publishProgress(Integer.valueOf(0), Integer.valueOf((int) p));
										lastPublishTime = System.currentTimeMillis();
									}
								}
							}
						}
						finally {
							bos.close();
						}
					}
				});
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

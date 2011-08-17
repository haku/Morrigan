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

package com.vaguehope.morrigan.android.tasks;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.android.Constants;
import com.vaguehope.morrigan.android.helper.ChecksumHelper;
import com.vaguehope.morrigan.android.helper.HttpFileDownloadHandler;
import com.vaguehope.morrigan.android.helper.HttpHelper;
import com.vaguehope.morrigan.android.helper.HttpFileDownloadHandler.DownloadProgressListener;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistReference;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class DownloadMediaTask extends AsyncTask<MlistItem, Integer, String> implements OnClickListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final MlistReference mlistReference;

	private ProgressDialog progressDialog;
	protected AtomicBoolean cancelled = new AtomicBoolean(false);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public DownloadMediaTask (Context context, MlistReference mlistReference) {
		this.context = context;
		this.mlistReference = mlistReference;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Context getContext () {
		return this.context;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static int PRGMAX = 10000;
	
	@Override
	protected void onPreExecute () {
		ProgressDialog dialog = new ProgressDialog(this.context);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setCancelable(false);
		dialog.setIndeterminate(false);
		dialog.setMax(PRGMAX);
		dialog.setTitle("Downloading...");
//		progressDialog.setProgressNumberFormat(null); // Not available 'till API 11 (3.x).
		
		dialog.setCancelable(true);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", this);
		
		dialog.show();
		this.progressDialog = dialog;
	}
	
	@Override
	public void onClick (DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_NEGATIVE) {
			if (DownloadMediaTask.this.cancelled.compareAndSet(false, true)) {
				DownloadMediaTask.this.progressDialog.setCancelable(false);
			}
		}
	}
	
	@Override
	protected String doInBackground (MlistItem... items) {
		if (items.length < 1) throw new IllegalArgumentException("No items desu~");
		
		final int pPerItem = (int) (PRGMAX / (float)items.length);
		final AtomicInteger itemsCopied = new AtomicInteger(0);
		
		DownloadProgressListener progressListener = new DownloadProgressListener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void downloadProgress (int bytesRead, int totalBytes) {
				float p = (bytesRead / (float)totalBytes) * pPerItem;
				publishProgress(Integer.valueOf(0), Integer.valueOf((pPerItem * itemsCopied.get()) + (int) p));
			}
			@Override
			public boolean abortListener () {
				return DownloadMediaTask.this.cancelled.get();
			}
		};
		
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File (sdCard.getAbsolutePath() + "/morrigan"); // TODO make this configurable.
		dir.mkdirs();
		
		final ByteBuffer byteBuffer = ChecksumHelper.createByteBuffer();
		
		for (MlistItem item : items) {
			final String url = this.mlistReference.getBaseUrl() + Constants.CONTEXT_MLIST_ITEMS + "/" + item.getRelativeUrl();
			final File file = new File(dir, item.getFileName());
			boolean transferComplete = false;
			
			// Will only skip if checksums really match.
			if (!fileMatchedItem(file, item, false, byteBuffer)) {
				try {
					HttpHelper.getUrlContent(url, new HttpFileDownloadHandler(file, progressListener));
					transferComplete = true;
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				finally {
					// If the current file is defiantly not valid delete it.
					if (!transferComplete || this.cancelled.get()) {
						if (file.exists() && !fileMatchedItem(file, item, false, byteBuffer)) {
							Log.i(Constants.LOGTAG, "Deleting incomplete file: " + file.getAbsolutePath());
							file.delete();
						}
					}
				}
			}
			
			// This will only fail if the checksums really do not match.
			if (!fileMatchedItem(file, item, true, byteBuffer)) return "Checksum check failed.";
			
			publishProgress(Integer.valueOf(pPerItem * itemsCopied.incrementAndGet()));
			if (this.cancelled.get()) return "Download cancelled desu~";
		}
		
		return "Download complete desu~";
	}
	
	@Override
	protected void onProgressUpdate (Integer... values) {
		// Note that one is increment and one is set.
		if (values.length >= 1 && values[0] != null && values[0].intValue() > 0) {
			this.progressDialog.setProgress(values[0].intValue());
		}
		if (values.length >= 2 && values[1] != null && values[1].intValue() > 0) {
			this.progressDialog.setSecondaryProgress(values[1].intValue());
		}
	}
	
	@Override
	protected void onPostExecute (String result) {
		if (this.progressDialog != null) this.progressDialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
		Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * defaultResponse is returned if comparison is not possible.
	 */
	private static boolean fileMatchedItem (File file, MlistItem item, boolean defaultResponse, ByteBuffer byteBuffer) {
		if (file.exists() && item.getHashCode() != null && !item.getHashCode().equals(BigInteger.ZERO)) {
			try {
				BigInteger hash = ChecksumHelper.generateMd5Checksum(file, byteBuffer);
				return hash.equals(item.getHashCode());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return defaultResponse;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

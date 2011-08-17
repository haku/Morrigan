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

package com.vaguehope.morrigan.android.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.vaguehope.morrigan.android.helper.HttpHelper.HttpStreamHandler;


public class HttpFileDownloadHandler implements HttpStreamHandler<RuntimeException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int DOWNLOADBUFFERSIZE = 8192;
	private static final int GUIUPDATEINTERVALMILLIS = 500;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static interface DownloadProgressListener {
		
		public void downloadProgress (int bytesRead, int totalBytes);
		
		/**
		 * Return true to abort download.
		 */
		public boolean abortListener ();
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final File file;
	private final DownloadProgressListener listener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public HttpFileDownloadHandler (File file, DownloadProgressListener listener) {
		this.file = file;
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void handleStream (InputStream is, int contentLength) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(this.file));
		
		// TODO this could probably be done better.
		try {
			byte[] buffer = new byte[DOWNLOADBUFFERSIZE];
			int bytesRead;
			int totalBytesRead = 0;
			long lastPublishTime = 0;
			while ((bytesRead = bis.read(buffer)) != -1) {
				bos.write(buffer, 0, bytesRead);
				totalBytesRead = totalBytesRead + bytesRead;
				if (contentLength > 0 && System.currentTimeMillis() - lastPublishTime > GUIUPDATEINTERVALMILLIS) {
					this.listener.downloadProgress(totalBytesRead, contentLength);
					lastPublishTime = System.currentTimeMillis();
					if (this.listener.abortListener()) is.close();
				}
			}
		}
		finally {
			bos.close();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

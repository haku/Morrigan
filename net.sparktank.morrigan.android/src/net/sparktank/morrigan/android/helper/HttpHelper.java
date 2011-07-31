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

package net.sparktank.morrigan.android.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	public static final int HTTP_READ_TIMEOUT_SECONDS = 60;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public interface HttpStreamHandler <T extends Exception> {
		
		public void handleStream (InputStream is, int contentLength) throws IOException, T;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String getUrlContent (String sUrl) throws IOException {
		return getUrlContent(sUrl, null, null, null);
	}
	
	public static String getUrlContent (String sUrl, String httpRequestMethod, String encodedData, String contentType) throws IOException {
		return getUrlContent(sUrl, httpRequestMethod, encodedData, contentType, (HttpStreamHandler<RuntimeException>) null);
	}
	
	public static <T extends Exception> String getUrlContent (String sUrl, HttpStreamHandler<T> streamHandler) throws IOException, T {
		return getUrlContent(sUrl, null, null, null, streamHandler);
	}
	
	public static <T extends Exception> String getUrlContent (String sUrl, String httpRequestMethod, String encodedData, String contentType, HttpStreamHandler<T> streamHandler) throws IOException, T {
		URL url = new URL(sUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);
		connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS * 1000);
		connection.setReadTimeout(HTTP_READ_TIMEOUT_SECONDS * 1000);
		
		if (httpRequestMethod != null) {
			connection.setRequestMethod(httpRequestMethod);
		}
		
		if (encodedData != null) {
			if (contentType!=null) connection.setRequestProperty("Content-Type", contentType);
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			try {
				out.write(encodedData);
				out.flush();
			} finally {
				out.close();
			}
		}
		
		StringBuilder sb = null;
		InputStream is = null;
		try {
			is = connection.getInputStream();
			
			if (streamHandler != null) {
				streamHandler.handleStream(is, connection.getContentLength());
			}
			else {
				sb = new StringBuilder();
				buildString(is, sb);
			}
		}
		finally {
			if (is != null) is.close();
		}
		
		return sb == null ? null : sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static void buildString (InputStream is, StringBuilder sb) throws IOException {
//		int v;
//		sb = new StringBuilder();
//		while( (v = is.read()) != -1){
//			sb.append((char)v);
//		}
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

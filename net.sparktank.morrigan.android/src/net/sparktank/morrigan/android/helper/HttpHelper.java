package net.sparktank.morrigan.android.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	public static final int HTTP_READ_TIMEOUT_SECONDS = 60;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String getUrlContent (String sUrl) throws IOException {
		URL url = new URL(sUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);
		connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS * 1000);
		connection.setReadTimeout(HTTP_READ_TIMEOUT_SECONDS * 1000);
		connection.connect();
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

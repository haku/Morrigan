package com.vaguehope.morrigan.danbooru;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.util.PropertiesFile;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

public class Danbooru {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static public String[] getTags (String md5) throws IOException, MorriganException, HttpStreamHandlerException {
		String surl = "http://danbooru.donmai.us/post/index.xml?" + getAuthString() + "tags=md5:" + md5;
		URL url = new URL(surl);
		HttpResponse response = HttpClient.doHttpRequest(url);

		if (response.getCode() != 200) {
			throw new MorriganException("Danbooru returned code " + response.getCode() + ".\n\n" + response.getBody());
		}

		if (response.getBody().contains("<posts count=\"1\"")) {
			String tagstring = substringByTokens(response.getBody(), "tags=\"", "\"");
			String[] tags = tagstring.split(" ");
			return tags;
		}

		return null;
	}

	/**
	 * Not sure what the max number of searches is...
	 * @param md5s
	 *            list of md5s to query for.
	 * @return Map where key=md5, value = tags.
	 * @throws IOException
	 * @throws MorriganException
	 * @throws HttpStreamHandlerException
	 */
	static public Map<String, String[]> getTags (Collection<String> md5s) throws IOException, MorriganException, HttpStreamHandlerException {
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://danbooru.donmai.us/post/index.xml?" + getAuthString() + "tags=md5:");
		boolean first = true;
		for (String md5 : md5s) {
			if (!first) urlString.append(",");
			first = false;

			urlString.append(md5);
		}

		URL url = new URL(urlString.toString());
		HttpResponse response = HttpClient.doHttpRequest(url);
		if (response.getCode() != 200) {
			throw new MorriganException("Danbooru returned code " + response.getCode() + ".\n\n" + response.getBody());
		}

		if (response.getBody().contains("<posts count=\"0\"")) { // No results.
			return new HashMap<String, String[]>();
		}

		String[] results = response.getBody().split("<post "); // The space is important, stops it matching "<posts".
		Map<String, String[]> ret = new HashMap<String, String[]>();
		for (String result : results) {
			if (result.contains("md5=\"")) {
				String md5 = substringByTokens(result, "md5=\"", "\"");
				String tagstring = substringByTokens(result, "tags=\"", "\"");
				String[] tags = tagstring.split(" ");
				ret.put(md5, tags);
			}
		}

		return ret;
	}

	private static String getAuthString () throws IOException {
		StringBuilder s = new StringBuilder();
		PropertiesFile propFile = new PropertiesFile(System.getProperty("user.home") + "/.danbooru");
		for (Entry<Object, Object> e : propFile.getAll()) {
			s.append(e.getKey().toString());
			s.append("=");
			s.append(e.getValue().toString());
			s.append("&");
		}
		return s.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static public String substringByTokens (String d, String k0, String k1) {
		String ret;
		int x0;
		int l;

		try {
			if (k0 == null) {
				x0 = 0;
				l = 0;
			}
			else {
				x0 = d.indexOf(k0);
				if (x0 < 0) throw new IllegalArgumentException("k0 '" + k0 + "' not found in '" + d + "'.");
				l = k0.length();
			}

			if (k1 != null) {
				int x1 = d.indexOf(k1, x0 + l + 1);
				if (x1 < 0) throw new IllegalArgumentException("k1 '" + k1 + "' not found in '" + d + "'.");
				ret = d.substring(x0 + l, x1);
			}
			else {
				ret = d.substring(x0 + l);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("data='" + d + "' k0='" + k0 + "' k1='" + k1 + "'.", e);
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

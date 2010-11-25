package net.sparktank.morrigan.danbooru;

import java.io.IOException;
import java.net.URL;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.HttpClient.HttpResponse;

public class Danbooru {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public String[] getTags (String md5) throws IOException, MorriganException {
		String surl = "http://danbooru.donmai.us/post/index.xml?tags=md5:" + md5;
		URL url = new URL(surl);
		HttpResponse response = HttpClient.getHttpClient().doHttpRequest(url);
		
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
				if (x0 < 0) throw new IllegalArgumentException("k0 '"+k0+"' not found in '"+d+"'.");
				l = k0.length();
			}

			if (k1 != null) {
				int x1 = d.indexOf(k1, x0+l+1);
				if (x1 < 0) throw new IllegalArgumentException("k1 '"+k1+"' not found in '"+d+"'.");
				ret = d.substring(x0+l, x1);
			}
			else {
				ret = d.substring(x0+l);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("data='"+d+"' k0='"+k0+"' k1='"+k1+"'.", e);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

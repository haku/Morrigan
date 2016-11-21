package com.vaguehope.morrigan.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicReference;

import org.xml.sax.SAXException;

import com.vaguehope.morrigan.android.helper.HttpHelper;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpStreamHandler;
import com.vaguehope.morrigan.android.model.MlistItemList;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.modelimpl.MlistItemListImpl;

public class MnApi {

	private MnApi () {}

	public static MlistItemList fetchDbItems (final ServerReference host, final String dbRelativePath, final String query) throws IOException {
		final String url = host.getBaseUrl() + dbRelativePath + C.CONTEXT_MLIST_QUERY + "/" + URLEncoder.encode(query) + "?maxresults=0";
		try {
			final AtomicReference<MlistItemList> res = new AtomicReference<MlistItemList>();
			HttpHelper.getUrlContent(url, "GET", null, null,
					new HttpStreamHandler<SAXException>() {
						@Override
						public void handleStream (final InputStream is, final int contentLength) throws IOException, SAXException {
							res.set(new MlistItemListImpl(is, query));
						}
					},
					host);
			return res.get();
		}
		catch (final IOException e) {
			throw new IOException("Failed to fetch '" + url + "': " + e.getMessage(), e);
		}
		catch (final SAXException e) {
			throw new IOException("Failed to parse response from '" + url + "': " + e.toString(), e);
		}
	}

}

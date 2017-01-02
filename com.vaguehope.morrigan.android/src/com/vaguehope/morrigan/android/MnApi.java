package com.vaguehope.morrigan.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicReference;

import org.xml.sax.SAXException;

import com.vaguehope.morrigan.android.helper.HttpFileDownloadHandler;
import com.vaguehope.morrigan.android.helper.HttpFileDownloadHandler.DownloadProgressListener;
import com.vaguehope.morrigan.android.helper.HttpHelper;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpStreamHandler;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistItemList;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.modelimpl.MlistItemListImpl;
import com.vaguehope.morrigan.android.modelimpl.MlistStateXmlImpl;
import com.vaguehope.morrigan.android.tasks.AbstractTask;

public class MnApi {

	private MnApi () {}

	public static MlistItemList fetchDbItems (final ServerReference host, final String dbRelativePath, final String query, final String transcode) throws IOException {
		final String url = host.getBaseUrl() + dbRelativePath + C.CONTEXT_MLIST_QUERY + "/" + URLEncoder.encode(query)
				+ "?maxresults=0"
				+ (transcode != null ? "&transcode=" + transcode : "");
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

	public static MlistState fetchDbSrcs (final ServerReference host, final String dbRelativePath) throws IOException {
		final String url = host.getBaseUrl() + dbRelativePath + C.CONTEXT_MLIST_SRC;
		try {
			final AtomicReference<MlistState> res = new AtomicReference<MlistState>();
			HttpHelper.getUrlContent(url, "GET", null, null,
					new HttpStreamHandler<SAXException>() {
						@Override
						public void handleStream (final InputStream is, final int contentLength) throws IOException, SAXException {
							res.set(new MlistStateXmlImpl(AbstractTask.parseStreamToString(is), host));
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

	public static void postToFile (final ServerReference host, final String dbRelativePath, final MlistItem item,
			final String action) throws IOException {
		final String url = host.getBaseUrl() + dbRelativePath + C.CONTEXT_MLIST_ITEMS + "/" + item.getRelativeUrl();
		try {
			HttpHelper.getUrlContent(url, "POST", "action=" + action, "application/x-www-form-urlencoded", host);
		}
		catch (final IOException e) {
			throw new IOException("Failed to POST '" + url + "': " + e.getMessage(), e);
		}
	}

	public static void downloadFile (final ServerReference host, final String dbRelativePath, final MlistItem item,
			final File localFile, final DownloadProgressListener progressListener) throws IOException {
		final String url = host.getBaseUrl() + dbRelativePath + C.CONTEXT_MLIST_ITEMS + "/" + item.getRelativeUrl();
		try {
			HttpHelper.getUrlContent(url, new HttpFileDownloadHandler(localFile, progressListener), host);
		}
		catch (final IOException e) {
			throw new IOException("Failed to fetch '" + url + "': " + e.getMessage(), e);
		}
	}

}

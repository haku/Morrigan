package com.vaguehope.morrigan.server.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.NotImplementedException;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.model.media.RemoteMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.db.AbstractMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.MediaDbConfig;
import com.vaguehope.morrigan.server.MlistsServlet;
import com.vaguehope.morrigan.server.feedreader.MediaDbFeedReader;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

public class RemoteMediaDbImpl extends AbstractMediaDb implements RemoteMediaDb {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	public static final String DBKEY_SERVERURL = "SERVERURL";
	public static final String DBKEY_CACHEDATE = "CACHEDATE";
	public static final String DBKEY_PASS = "PASS";

	public static final long MAX_CACHE_AGE = 60 * 60 * 1000L; // 1 hour.

	private final RemoteHostDetails remoteHostDetails;

	private long cacheDate = -1;
	private TaskEventListener taskEventListener;


	public RemoteMediaDbImpl (final String dbName, final MediaDbConfig config, final RemoteHostDetails details) throws DbException {
		super(dbName, config); // TODO expose search term.
		this.remoteHostDetails = details;
	}

	@Override
	public void setDbLayer(MediaStorageLayer dbLayer) throws DbException {
		super.setDbLayer(dbLayer);

		// Create a fresh DB.
		if (this.remoteHostDetails != null) {
			setUri(this.remoteHostDetails.getUri());
			setPass(this.remoteHostDetails.getPass() != null ? this.remoteHostDetails.getPass() : "");
		}
		readCacheDate();
	}

	private void readCacheDate () throws DbException {
		String dateString = getDbLayer().getProp(DBKEY_CACHEDATE);
		if (dateString != null) {
			long date = Long.parseLong(dateString);
			if (date > 0) {
				this.cacheDate = date;
				this.logger.fine("Read cachedate=" + this.cacheDate + ".");
			}
		}
		else {
			this.cacheDate = -1;
		}
	}

	private void writeCacheDate () throws DbException {
		getDbLayer().setProp(DBKEY_CACHEDATE, String.valueOf(this.cacheDate));
		this.logger.fine("Wrote cachedate=" + this.cacheDate + ".");
	}

	@Override
	public URI getUri() throws DbException {
		final String sUrl = getDbLayer().getProp(DBKEY_SERVERURL);
		try {
			return new URI(sUrl);
		}
		catch (final URISyntaxException e) {
			throw new DbException("URL in DB is malformed: " + sUrl, e);
		}
	}

	@Override
	public void setUri (final URI uri) throws DbException {
		getDbLayer().setProp(DBKEY_SERVERURL, uri.toString());
	}

	@Override
	public String getPass () throws DbException {
		return getDbLayer().getProp(DBKEY_PASS);
	}

	@Override
	public void setPass (final String pass) throws DbException {
		getDbLayer().setProp(DBKEY_PASS, pass);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MediaListType getType() {
		return MediaListType.REMOTEMMDB;
	}

	@Override
	public TaskEventListener getTaskEventListener() {
		return this.taskEventListener;
	}
	@Override
	public void setTaskEventListener(final TaskEventListener taskEventListener) {
		this.taskEventListener = taskEventListener;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Reading and refreshing.

	@Override
	public long getCacheAge () {
		return System.currentTimeMillis() - this.cacheDate;
	}

	@Override
	public boolean isCacheExpired () {
		return (getCacheAge() > MAX_CACHE_AGE); // 1 hour.  TODO extract this as config.
	}

	@Override
	public void readFromCache () throws DbException, MorriganException {
		super.doRead();
	}

	@Override
	protected void doRead() throws DbException, MorriganException {
		if (isCacheExpired()) {
			this.logger.info("Cache for '" + getListName() + "' is " + getCacheAge() + "ms old, fetching data...");
				forceDoRead();
		}
		else {
			this.logger.fine("Not refreshing as '" + getListName() + "' cache is only " + getCacheAge() + "ms old.");
			super.doRead(); // This forces a DB query - sorts entries.
		}
	}

	@Override
	public void forceDoRead () throws MorriganException, DbException {
		try {
			// This does the actual HTTP fetch.
			MediaDbFeedReader.read(this, this.taskEventListener);

			this.cacheDate = System.currentTimeMillis();
			writeCacheDate();
		}
		finally {
			super.doRead(); // This forces a DB query - sorts entries.)
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.

	@Override
	public void copyItemFile (final MediaItem item, final OutputStream os) throws MorriganException {
		URL itemUrl = getRemoteItemUrl(this, item);
		this.logger.fine("Fetching '" + itemUrl + "' to '" + item.getFilepath() + "'...");
		try {
			HttpClient.downloadFile(itemUrl, os);
		}
		catch (IOException e) {
			if (e instanceof UnknownHostException) {
				throw new MorriganException("Host unknown.", e);
			}
			else if (e instanceof SocketException) {
				throw new MorriganException("Host unreachable.", e);
			}
			else {
				throw new MorriganException(e);
			}
		}
		catch (HttpStreamHandlerException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public File copyItemFile (final MediaItem mlt, final File targetDirectory) throws MorriganException {
		if (!targetDirectory.isDirectory()) {
			throw new IllegalArgumentException("targetDirectory must be a directory.");
		}

		final File targetFile = new File(targetDirectory.getAbsolutePath() + File.separatorChar
				+ mlt.getFilepath().substring(mlt.getFilepath().lastIndexOf(File.separatorChar) + 1));

		if (!targetFile.exists()) {
			URL itemUrl = getRemoteItemUrl(this, mlt);
			this.logger.fine("Fetching '"+itemUrl+"' to '"+targetFile.getAbsolutePath()+"'...");
			try {
				HttpClient.downloadFile(itemUrl, targetFile);
			}
			catch (IOException e) {
				if (e instanceof UnknownHostException) {
					throw new MorriganException("Host unknown.", e);
				}
				else if (e instanceof SocketException) {
					throw new MorriganException("Host unreachable.", e);
				}
				else {
					throw new MorriganException(e);
				}
			} catch (HttpStreamHandlerException e) {
				throw new MorriganException(e);
			}
		}
		else {
			this.logger.warning("Skipping '"+targetFile.getAbsolutePath()+"' as it already exists.");
		}

		return targetFile;
	}

	private static URL getRemoteItemUrl (final RemoteMediaDbImpl rmmdb, final MediaItem mlt) throws MorriganException {
		String serverUrlString;
		try {
			serverUrlString = rmmdb.getDbLayer().getProp(DBKEY_SERVERURL); // e.g. http://localhost:8080/mlists/REMOTEMMDB/localhost_8080_wui.remote.db3
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
		URL serverUrl;
		try {
			serverUrl = new URL(serverUrlString);
		}
		catch (MalformedURLException e) {
			throw new MorriganException(e);
		}

		URL itemUrl;
		try {
			// need to prepend "/items/".
			String remoteLocation = serverUrl.getFile() + "/" + MlistsServlet.PATH_ITEMS + "/" + mlt.getRemoteLocation();
			itemUrl = new URL(serverUrl.getProtocol(), serverUrl.getHost(), serverUrl.getPort(), remoteLocation);
		}
		catch (MalformedURLException e) {
			throw new MorriganException(e);
		}
		return itemUrl;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Item metadata modifiers.

	@Override
	public void incTrackStartCnt (final MediaItem track, final long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void incTrackEndCnt (final MediaItem track, final long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemDateAdded (final MediaItem track, final Date date) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackDateLastPlayed (final MediaItem track, final Date date) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void removeItem (final MediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void incTrackStartCnt(final MediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void incTrackEndCnt(final MediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackStartCnt(final MediaItem item, final long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackEndCnt(final MediaItem item, final long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackDuration(final MediaItem track, final int duration) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemMd5(final MediaItem track, final BigInteger md5) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemSha1(MediaItem item, BigInteger sha1) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemDateLastModified(final MediaItem track, final Date date) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemEnabled(final MediaItem track, final boolean value) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemEnabled (final MediaItem item, final boolean value, final Date lastModified) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemMissing(final MediaItem track, final boolean value) throws MorriganException {
		throw new NotImplementedException();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB property modifiers.

	@Override
	public void addSource (final String source) throws MorriganException {
		throw new NotImplementedException ("Not implemented.");
	}

	@Override
	public void removeSource (final String source) throws MorriganException {
		throw new NotImplementedException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.

	// TODO FIXME stop user adding tags?

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

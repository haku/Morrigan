package com.vaguehope.morrigan.server.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.NotImplementedException;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.AbstractMixedMediaDb;
import com.vaguehope.morrigan.server.MlistsServlet;
import com.vaguehope.morrigan.server.feedreader.MixedMediaDbFeedReader;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;
import com.vaguehope.sqlitewrapper.DbException;

public class RemoteMixedMediaDb extends AbstractMixedMediaDb implements IRemoteMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Logger logger = Logger.getLogger(this.getClass().getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String DBKEY_SERVERURL = "SERVERURL";
	public static final String DBKEY_CACHEDATE = "CACHEDATE";
	public static final String DBKEY_PASS = "PASS";

	public static long MAX_CACHE_AGE = 60 * 60 * 1000L; // 1 hour.
	private long cacheDate = -1;

	private TaskEventListener taskEventListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Connect to existing DB.
	 */
	public RemoteMixedMediaDb (String dbName, MediaItemDbConfig config, IMixedMediaStorageLayer<IMixedMediaItem> localDbLayer) throws DbException {
		super(dbName, config, localDbLayer); // TODO expose search term.
		readCacheDate();
	}

	/**
	 * Create a fresh DB.
	 */
	public RemoteMixedMediaDb (String dbName, MediaItemDbConfig config, RemoteHostDetails details, IMixedMediaStorageLayer<IMixedMediaItem> localDbLayer) throws DbException {
		super(dbName, config, localDbLayer); // TODO expose search term.
		URL url = details.getUrl();

		String s = getDbLayer().getProp(DBKEY_SERVERURL);
		if (s == null) {
			setUrl(url);
			this.logger.fine("Set DBKEY_SERVERURL=" + url.toExternalForm() + " in " + getDbLayer().getDbFilePath());

		} else if (!s.equals(url.toExternalForm())) {
			throw new IllegalArgumentException("serverUrl does not match localDbLayer ('"+url.toExternalForm()+"' != '"+s+"' in '"+getDbLayer().getDbFilePath()+"').");
		}

		setPass(details.getPass());
		readCacheDate();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

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
	public URL getUrl() throws DbException {
		String sUrl = getDbLayer().getProp(DBKEY_SERVERURL);
		try {
			return new URL(sUrl);
		}
		catch (MalformedURLException e) {
			throw new DbException("URL in DB is malformed: " + sUrl, e);
		}
	}

	@Override
	public void setUrl (URL url) throws DbException {
		getDbLayer().setProp(DBKEY_SERVERURL, url.toExternalForm());
	}

	@Override
	public String getPass () throws DbException {
		return getDbLayer().getProp(DBKEY_PASS);
	}

	@Override
	public void setPass (String pass) throws DbException {
		getDbLayer().setProp(DBKEY_PASS, pass);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public TaskEventListener getTaskEventListener() {
		return this.taskEventListener;
	}
	@Override
	public void setTaskEventListener(TaskEventListener taskEventListener) {
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
			MixedMediaDbFeedReader.read(this, this.taskEventListener);

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
	public void copyItemFile (IMixedMediaItem item, OutputStream os) throws MorriganException {
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
	public File copyItemFile (IMixedMediaItem mlt, File targetDirectory) throws MorriganException {
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

	static public URL getRemoteItemUrl (RemoteMixedMediaDb rmmdb, IMixedMediaItem mlt) throws MorriganException {
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
	public void incTrackStartCnt (IMediaTrack track, long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void incTrackEndCnt (IMediaTrack track, long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemDateAdded (IMixedMediaItem track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackDateLastPlayed (IMediaTrack track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void removeItem (IMixedMediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void incTrackStartCnt(IMediaTrack track) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void incTrackEndCnt(IMediaTrack track) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackStartCnt(IMediaTrack item, long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackEndCnt(IMediaTrack item, long n) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setTrackDuration(IMediaTrack track, int duration) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemHashCode(IMixedMediaItem track, BigInteger hashcode) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemDateLastModified(IMixedMediaItem track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemEnabled(IMixedMediaItem track, boolean value) throws MorriganException {
		throw new NotImplementedException();
	}

	@Override
	public void setItemMissing(IMixedMediaItem track, boolean value) throws MorriganException {
		throw new NotImplementedException();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB property modifiers.

	@Override
	public void addSource (String source) throws MorriganException {
		throw new NotImplementedException ("Not implemented.");
	}

	@Override
	public void removeSource (String source) throws MorriganException {
		throw new NotImplementedException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.

	// TODO FIXME stop user adding tags?

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

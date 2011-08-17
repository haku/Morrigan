package net.sparktank.morrigan.server.model;

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

import com.vaguehope.sqlitewrapper.DbException;

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.internal.db.MediaItemDbConfig;
import net.sparktank.morrigan.model.media.internal.db.mmdb.AbstractMixedMediaDb;
import net.sparktank.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerFactory;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.server.MlistsServlet;
import net.sparktank.morrigan.server.feedreader.MixedMediaDbFeedParser;
import net.sparktank.morrigan.util.httpclient.HttpClient;
import net.sparktank.morrigan.util.httpclient.HttpStreamHandlerException;

public class RemoteMixedMediaDb extends AbstractMixedMediaDb<IRemoteMixedMediaDb> implements IRemoteMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
	public static class RemoteMixedMediaDbFactory extends RecyclingFactory<IRemoteMixedMediaDb, String, URL, MorriganException> {
		
		protected RemoteMixedMediaDbFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(IRemoteMixedMediaDb product) {
//			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected IRemoteMixedMediaDb makeNewProduct(String material) throws MorriganException {
			return makeNewProduct(material, null);
		}
		
		@Override
		protected IRemoteMixedMediaDb makeNewProduct(String material, URL config) throws MorriganException {
			IRemoteMixedMediaDb ret = null;
			
//			System.out.println("Making object instance '" + material + "'...");
			if (config != null) {
				try {
					ret = new RemoteMixedMediaDb(
							RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material),
							new MediaItemDbConfig(material, null),
							config,
							MixedMediaSqliteLayerFactory.getAutocommit(material));
				}
				catch (DbException e) {
					throw new MorriganException(e);
				}
			} else {
				try {
					ret = new RemoteMixedMediaDb(
							RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material),
							new MediaItemDbConfig(material, null),
							MixedMediaSqliteLayerFactory.getAutocommit(material));
				}
				catch (MalformedURLException e) {
					throw new MorriganException(e);
				} catch (DbException e) {
					throw new MorriganException(e);
				}
			}
			
			return ret;
		}
		
	}
	
	public static final RemoteMixedMediaDbFactory FACTORY = new RemoteMixedMediaDbFactory();
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String DBKEY_SERVERURL = "SERVERURL";
	public static final String DBKEY_CACHEDATE = "CACHEDATE";
	
	private final URL url;
	
	private TaskEventListener taskEventListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RemoteMixedMediaDb (String dbName, MediaItemDbConfig config, IMixedMediaStorageLayer<IMixedMediaItem> localDbLayer) throws DbException, MalformedURLException {
		super(dbName, config, localDbLayer); // TODO expose search term.
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s != null) {
			this.url = new URL(s);
		}
		else {
			throw new IllegalArgumentException("serverUrl not found in localDbLayer ('"+localDbLayer.getDbFilePath()+"').");
		}
		
		readCacheDate();
	}
	
	public RemoteMixedMediaDb (String dbName, MediaItemDbConfig config,  URL url, IMixedMediaStorageLayer<IMixedMediaItem> localDbLayer) throws DbException {
		super(dbName, config, localDbLayer); // TODO expose search term.
		this.url = url;
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s == null) {
			localDbLayer.setProp(DBKEY_SERVERURL, url.toExternalForm());
			this.logger.fine("Set DBKEY_SERVERURL=" + url.toExternalForm() + " in " + localDbLayer.getDbFilePath());
			
		} else if (!s.equals(url.toExternalForm())) {
			throw new IllegalArgumentException("serverUrl does not match localDbLayer ('"+url.toExternalForm()+"' != '"+s+"' in '"+localDbLayer.getDbFilePath()+"').");
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public RemoteMixedMediaDb getTransactionalClone() throws DbException {
		return new RemoteMixedMediaDb(
				RemoteMixedMediaDbHelper.getRemoteMmdbTitle(getDbPath()),
				new MediaItemDbConfig(getDbPath(), null),
				getUrl(), MixedMediaSqliteLayerFactory.getTransactional(getDbPath())
				);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public URL getUrl() {
		return this.url;
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
	
	public static long MAX_CACHE_AGE = 60 * 60 * 1000;
	
	private long cacheDate = -1;
	
	@Override
	public long getCacheAge () {
		return new Date().getTime() - this.cacheDate;
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
			this.logger.info("Cache for '" + getListName() + "' is " + getCacheAge() + " old, reading data from " + this.url.toExternalForm() + " ...");
				forceDoRead();
		}
		else {
			this.logger.info("Not refreshing as '" + getListName() + "' cache is only " + getCacheAge() + " old.");
			super.doRead(); // This forces a DB query - sorts entries.)
		}
	}
	
	@Override
	public void forceDoRead () throws MorriganException, DbException {
		try {
			// This does the actual HTTP fetch.
			MixedMediaDbFeedParser.parseFeed(this, this.taskEventListener);
			
			this.cacheDate = new Date().getTime();
			writeCacheDate();
			
		} finally {
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
			HttpClient.getHttpClient().downloadFile(itemUrl, os);
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
				HttpClient.getHttpClient().downloadFile(itemUrl, targetFile);
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

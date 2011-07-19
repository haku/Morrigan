package net.sparktank.morrigan.server.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.model.db.IDbItem;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaTag;
import net.sparktank.morrigan.model.media.MediaTagClassification;
import net.sparktank.morrigan.model.media.MediaTagType;
import net.sparktank.morrigan.model.media.internal.db.mmdb.AbstractMixedMediaDb;
import net.sparktank.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerFactory;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.server.feedreader.MixedMediaDbFeedParser;
import net.sparktank.morrigan.util.httpclient.HttpClient;
import net.sparktank.morrigan.util.httpclient.HttpStreamHandler;
import net.sparktank.morrigan.util.httpclient.HttpStreamHandlerException;
import net.sparktank.sqlitewrapper.DbException;

public class RemoteMixedMediaDb extends AbstractMixedMediaDb<IRemoteMixedMediaDb> implements IRemoteMixedMediaDb {
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
					ret = new RemoteMixedMediaDb(RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material), config, MixedMediaSqliteLayerFactory.INSTANCE.manufacture(material));
				} catch (DbException e) {
					throw new MorriganException(e);
				}
			} else {
				try {
					ret = new RemoteMixedMediaDb(RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material), MixedMediaSqliteLayerFactory.INSTANCE.manufacture(material));
				} catch (MalformedURLException e) {
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
	
	public RemoteMixedMediaDb (String dbName, IMixedMediaStorageLayer<IMixedMediaItem> localDbLayer) throws DbException, MalformedURLException {
		super(dbName, localDbLayer, null); // TODO expose search term.
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s != null) {
			this.url = new URL(s);
			
		} else {
			throw new IllegalArgumentException("serverUrl not found in localDbLayer ('"+localDbLayer.getDbFilePath()+"').");
		}
		
		readCacheDate();
	}
	
	public RemoteMixedMediaDb (String dbName,  URL url, IMixedMediaStorageLayer<IMixedMediaItem> localDbLayer) throws DbException {
		super(dbName, localDbLayer, null); // TODO expose search term.
		this.url = url;
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s == null) {
			localDbLayer.setProp(DBKEY_SERVERURL, url.toExternalForm());
			System.err.println("Set DBKEY_SERVERURL=" + url.toExternalForm() + " in " + localDbLayer.getDbFilePath());
			
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
				System.err.println("Read cachedate=" + this.cacheDate + ".");
			}
		}
		else {
			this.cacheDate = -1;
		}
	}
	
	private void writeCacheDate () throws DbException {
		getDbLayer().setProp(DBKEY_CACHEDATE, String.valueOf(this.cacheDate));
		System.err.println("Wrote cachedate=" + this.cacheDate + ".");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@SuppressWarnings("boxing")
	@Override
	public RemoteMixedMediaDb getTransactionalClone() throws DbException {
		return new RemoteMixedMediaDb(RemoteMixedMediaDbHelper.getRemoteMmdbTitle(getDbPath()), getUrl(), MixedMediaSqliteLayerFactory.INSTANCE.manufacture(getDbPath(), false, true));
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
			System.err.println("Cache for '" + getListName() + "' is " + getCacheAge() + " old, reading data from " + this.url.toExternalForm() + " ...");
				forceDoRead();
		}
		else {
			System.err.println("Not refreshing as '" + getListName() + "' cache is only " + getCacheAge() + " old.");
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
	public File copyItemFile(IMixedMediaItem mlt, File targetDirectory) throws MorriganException {
		if (!targetDirectory.isDirectory()) {
			throw new IllegalArgumentException("targetDirectory must be a directory.");
		}
		
		final File targetFile = new File(targetDirectory.getAbsolutePath() + File.separatorChar
				+ mlt.getFilepath().substring(mlt.getFilepath().lastIndexOf(File.separatorChar) + 1));
		
		if (!targetFile.exists()) {
			String serverUrlString;
			try {
				serverUrlString = getDbLayer().getProp(DBKEY_SERVERURL);
			} catch (DbException e) {
				throw new MorriganException(e);
			}
			URL serverUrl;
			try {
				serverUrl = new URL(serverUrlString);
			} catch (MalformedURLException e) {
				throw new MorriganException(e);
			}
			
			URL itemUrl;
			try {
				itemUrl = new URL(serverUrl.getProtocol(), serverUrl.getHost(), serverUrl.getPort(), mlt.getRemoteLocation());
			}
			catch (MalformedURLException e) {
				throw new MorriganException(e);
			}
			
			System.err.println("Fetching '"+itemUrl+"' to '"+targetFile.getAbsolutePath()+"'...");
			
			HttpStreamHandler httpStreamHandler = new HttpStreamHandler () {
				@Override
				public void handleStream(InputStream is) throws IOException, HttpStreamHandlerException {
					BufferedInputStream bis = new BufferedInputStream(is);
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
					
					// FIXME this could probably be done better.
					try {
						byte[] buffer = new byte[4096];
						int bytesRead;
						while ((bytesRead = bis.read(buffer)) != -1) {
							bos.write(buffer, 0, bytesRead);
						}
					}
					finally {
						bos.close();
					}
					
				}
			};
			
			try {
				HttpClient.getHttpClient().doHttpRequest(itemUrl, httpStreamHandler);
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
			System.err.println("Skipping '"+targetFile.getAbsolutePath()+"' as it already exists.");
		}
		
		return targetFile;
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
//	Db property modifiers.
	
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
	
	@Override
	public boolean hasTags (IDbItem item) throws MorriganException {
		return false;
	}
	
	@Override
	public List<MediaTag> getTags (IDbItem item) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void moveTags (IDbItem from_item, IDbItem to_item) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void removeTag (MediaTag mt) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void addTagClassification(String classificationName) throws MorriganException {
		throw new NotImplementedException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

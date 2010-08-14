package net.sparktank.morrigan.model.tracks.library.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.db.interfaces.IDbItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.morrigan.model.tracks.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.tracks.library.LibrarySqliteLayer2;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.HttpClient.IHttpStreamHandler;
import net.sparktank.morrigan.server.feedreader.MediaListFeedParser2;
import net.sparktank.sqlitewrapper.DbException;

public class RemoteMediaLibrary extends AbstractMediaLibrary {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "REMOTELIBRARY";
	
	public static final String DBKEY_SERVERURL = "SERVERURL";
	public static final String DBKEY_CACHEDATE = "CACHEDATE";

	private final URL url;

	private TaskEventListener taskEventListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RemoteMediaLibrary (String libraryName, LibrarySqliteLayer2 localDbLayer) throws DbException, MalformedURLException {
		super(libraryName, localDbLayer);
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s != null) {
			this.url = new URL(s);
			
		} else {
			throw new IllegalArgumentException("serverUrl not found in localDbLayer ('"+localDbLayer.getDbFilePath()+"').");
		}
		
		readCacheDate();
	}
	
	public RemoteMediaLibrary (String libraryName, URL url, LibrarySqliteLayer2 localDbLayer) throws DbException {
		super(libraryName, localDbLayer);
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
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	public URL getUrl() {
		return this.url;
	}
	
	public TaskEventListener getTaskEventListener() {
		return this.taskEventListener;
	}
	public void setTaskEventListener(TaskEventListener taskEventListener) {
		this.taskEventListener = taskEventListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Reading and refreshing.
	
	public static long MAX_CACHE_AGE = 60 * 60 * 1000;
	
	private long cacheDate = -1;
	
	public long getCacheAge () {
		return new Date().getTime() - this.cacheDate;
	}
	
	public boolean isCacheExpired () {
		return (getCacheAge() > MAX_CACHE_AGE); // 1 hour.  TODO extract this as config.
	}
	
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
	
	public void forceDoRead () throws MorriganException, DbException {
		try {
			// This does the actual HTTP fetch.
			MediaListFeedParser2.parseFeed(this, this.taskEventListener);
			
			this.cacheDate = new Date().getTime();
			writeCacheDate();
			
		} finally {
			super.doRead(); // This forces a DB query - sorts entries.)
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	@Override
	public void copyItemFile(IMediaTrack mlt, File targetDirectory) throws MorriganException {
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
			
			String itemUrlString =
				serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":" + serverUrl.getPort()
				+ mlt.getRemoteLocation();
			
			URL itemUrl;
			try {
				itemUrl = new URL(itemUrlString);
			} catch (MalformedURLException e) {
				throw new MorriganException(e);
			}
			
			System.err.println("Fetching '"+itemUrlString+"' to '"+targetFile.getAbsolutePath()+"'...");
			
			IHttpStreamHandler httpStreamHandler = new IHttpStreamHandler () {
				@Override
				public void handleStream(InputStream is) throws IOException, MorriganException {
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
			} catch (IOException e) {
				if (e instanceof UnknownHostException) {
					throw new MorriganException("Host unknown.", e);
				} else if (e instanceof SocketException) {
					throw new MorriganException("Host unreachable.", e);
				} else {
					throw new MorriganException(e);
				}
			}
		}
		else {
			System.err.println("Skipping '"+targetFile.getAbsolutePath()+"' as it already exists.");
		}
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
	public void setItemDateAdded (IMediaTrack track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackDateLastPlayed (IMediaTrack track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void removeItem (IMediaTrack track) throws MorriganException {
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
	public void setTrackDuration(IMediaTrack track, int duration) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setItemHashCode(IMediaTrack track, long hashcode) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setItemDateLastModified(IMediaTrack track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setItemEnabled(IMediaTrack track, boolean value) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setItemMissing(IMediaTrack track, boolean value) throws MorriganException {
		throw new NotImplementedException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Library property modifiers.
	
	@Override
	public void addSource (String source) throws DbException {
		throw new DbException("Not implemented.");
	}
	
	@Override
	public void removeSource (String source) throws DbException {
		throw new DbException("Not implemented.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.
	
	@Override
	public boolean hasTags (IDbItem item) throws MorriganException {
		throw new NotImplementedException();
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

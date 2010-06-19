package net.sparktank.morrigan.model.library.remote;

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

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaTrack;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.MediaLibraryTrack;
import net.sparktank.morrigan.model.library.SqliteLayer;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.HttpClient.IHttpStreamHandler;
import net.sparktank.morrigan.server.feedreader.MediaListFeedParser2;

public class RemoteMediaLibrary extends AbstractMediaLibrary {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "REMOTELIBRARY";
	
	public static final String DBKEY_SERVERURL = "SERVERURL";
	public static final String DBKEY_CACHEDATE = "CACHEDATE";

	private final URL url;

	private TaskEventListener taskEventListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RemoteMediaLibrary (String libraryName, SqliteLayer localDbLayer) throws DbException, MalformedURLException {
		super(libraryName, localDbLayer);
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s != null) {
			url = new URL(s);
			
		} else {
			throw new IllegalArgumentException("serverUrl not found in localDbLayer ('"+localDbLayer.getDbFilePath()+"').");
		}
		
		readCacheDate();
	}
	
	public RemoteMediaLibrary (String libraryName, URL url, SqliteLayer localDbLayer) throws DbException {
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
				cacheDate = date;
				System.err.println("Read cachedate=" + cacheDate + ".");
			}
		}
		else {
			cacheDate = -1;
		}
	}
	
	private void writeCacheDate () throws DbException {
		getDbLayer().setProp(DBKEY_CACHEDATE, String.valueOf(cacheDate));
		System.err.println("Wrote cachedate=" + cacheDate + ".");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	public URL getUrl() {
		return url;
	}
	
	public TaskEventListener getTaskEventListener() {
		return taskEventListener;
	}
	public void setTaskEventListener(TaskEventListener taskEventListener) {
		this.taskEventListener = taskEventListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Reading and refreshing.
	
	public static long MAX_CACHE_AGE = 60 * 60 * 1000;
	
	private long cacheDate = -1;
	
	public long getCacheAge () {
		return new Date().getTime() - cacheDate;
	}
	
	public boolean isCacheExpired () {
		return (getCacheAge() > MAX_CACHE_AGE); // 1 hour.  TODO extract this as config.
	}
	
	public void readFromCache () throws MorriganException {
		super.doRead();
	}
	
	@Override
	protected void doRead() throws MorriganException {
		if (isCacheExpired()) {
			System.err.println("Cache for '" + getListName() + "' is " + getCacheAge() + " old, reading data from " + url.toExternalForm() + " ...");
			forceDoRead();
		}
		else {
			System.err.println("Not refreshing as '" + getListName() + "' cache is only " + getCacheAge() + " old.");
			super.doRead(); // This forces a DB query - sorts entries.)
		}
	}
	
	public void forceDoRead () throws MorriganException {
		try {
			// This does the actual HTTP fetch.
			MediaListFeedParser2.parseFeed(this, taskEventListener);
			
			cacheDate = new Date().getTime();
			writeCacheDate();
			
		} finally {
			super.doRead(); // This forces a DB query - sorts entries.)
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	@Override
	public void copyMediaItemFile(MediaItem mi, File targetDirectory) throws MorriganException {
		if (mi instanceof MediaLibraryTrack) {
			if (!targetDirectory.isDirectory()) {
				throw new IllegalArgumentException("targetDirectory must be a directory.");
			}
			
			final File targetFile = new File(targetDirectory.getAbsolutePath() + File.separatorChar
					+ mi.getFilepath().substring(mi.getFilepath().lastIndexOf(File.separatorChar) + 1));
			
			if (!targetFile.exists()) {
				MediaLibraryTrack mlt = (MediaLibraryTrack) mi;
				
				String serverUrlString = getDbLayer().getProp(DBKEY_SERVERURL);
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
						
						// FIXME this is ubertastically slow.
						try {
							int i;
							while ((i = bis.read()) != -1) {
								bos.write(i);
							}
						} finally {
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
		else {
			super.copyMediaItemFile(mi, targetDirectory);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Item metadata modifiers.
	
	@Override
	public void incTrackStartCnt (MediaTrack track, long n) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void incTrackEndCnt (MediaTrack track, long n) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setDateAdded (MediaItem track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setDateLastPlayed (MediaItem track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void removeMediaTrack (MediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void incTrackStartCnt(MediaTrack track) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void incTrackEndCnt(MediaTrack track) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackDuration(MediaTrack track, int duration) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackHashCode(MediaItem track, long hashcode) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackDateLastModified(MediaItem track, Date date) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackEnabled(MediaItem track, boolean value) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackMissing(MediaItem track, boolean value) throws MorriganException {
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
}

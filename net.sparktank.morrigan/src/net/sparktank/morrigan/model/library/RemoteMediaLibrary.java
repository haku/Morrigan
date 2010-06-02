package net.sparktank.morrigan.model.library;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.server.feedreader.MediaListFeedParser2;

public class RemoteMediaLibrary extends AbstractMediaLibrary {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "REMOTELIBRARY";
	
	public static final String DBKEY_SERVERURL = "SERVERURL";

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
	
	private long cacheDate = -1;
	
	/**
	 * TODO this will be useful when I start pushing data back to the server.
	 */
	public void invalidateCache () {
		cacheDate = -1;
	}
	
	public void readFromCache () throws MorriganException {
		super.doRead();
	}
	
	@Override
	protected void doRead() throws MorriganException {
		long age = new Date().getTime() - cacheDate;
		if (age > 60 * 60 * 1000) { // 1 hour.  TODO extract this as config.
			if (cacheDate > 0) {
				System.err.println("Cache for '" + getListName() + "' is " + age + " old, reading data from " + url.toExternalForm() + " ...");
			} else {
				System.err.println("Cache invalidated, reading data from " + url.toExternalForm() + " ...");
			}
			
			try {
				// This does the actual HTTP fetch.
				MediaListFeedParser2.parseFeed(this, taskEventListener);
				
			} catch (Exception e) {
				if (e.getCause() instanceof UnknownHostException) {
					throw new MorriganException("Host unknown.", e);
					
				} else if (e.getCause() instanceof SocketException) {
					throw new MorriganException("Host unreachable.", e);
					
				} else {
					throw new MorriganException(e);
				}
			}
			
			super.doRead(); // This forces a DB query - sorts entries.
			
			cacheDate = new Date().getTime();
			
		} else {
			System.err.println("Not refreshing as '" + getListName() + "' cache is only " + age + " old.");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void incTrackStartCnt (MediaItem track, long n) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void incTrackEndCnt (MediaItem track, long n) throws MorriganException {
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
	public void incTrackStartCnt(MediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void incTrackEndCnt(MediaItem track) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void setTrackDuration(MediaItem track, int duration) throws MorriganException {
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

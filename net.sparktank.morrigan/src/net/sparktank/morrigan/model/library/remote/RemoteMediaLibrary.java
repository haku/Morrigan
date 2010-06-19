package net.sparktank.morrigan.model.library.remote;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaTrack;
import net.sparktank.morrigan.model.TaskEventListener;
import net.sparktank.morrigan.model.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.SqliteLayer;
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

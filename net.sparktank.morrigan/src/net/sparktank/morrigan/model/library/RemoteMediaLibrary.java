package net.sparktank.morrigan.model.library;

import java.io.File;
import java.net.URL;
import java.util.Date;

import net.sparktank.morrigan.engines.playback.NotImplementedException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.server.feedreader.MediaListFeedReader;

public class RemoteMediaLibrary extends AbstractMediaLibrary {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "REMOTELIBRARY";
	
	public static final String DBKEY_SERVERURL = "SERVERURL";

	private final String serverUrl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RemoteMediaLibrary (String libraryName, SqliteLayer localDbLayer) throws DbException {
		super(libraryName, localDbLayer);
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s != null) {
			this.serverUrl = s;
			
		} else {
			throw new IllegalArgumentException("serverUrl not found in localDbLayer ('"+localDbLayer.getDbFilePath()+"').");
		}
	}
	
	public RemoteMediaLibrary (String libraryName, String serverUrl, SqliteLayer localDbLayer) throws DbException {
		super(libraryName, localDbLayer);
		this.serverUrl = serverUrl;
		
		String s = localDbLayer.getProp(DBKEY_SERVERURL);
		if (s == null) {
			localDbLayer.setProp(DBKEY_SERVERURL, serverUrl);
			System.err.println("Set DBKEY_SERVERURL=" + serverUrl + " in " + localDbLayer.getDbFilePath());
			
		} else if (!s.equals(serverUrl)) {
			throw new IllegalArgumentException("serverUrl does not match localDbLayer ('"+serverUrl+"' != '"+s+"' in '"+localDbLayer.getDbFilePath()+"').");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doRead() throws MorriganException {
		System.err.println("Reading data from " + serverUrl + " ...");
		try {
			MediaListFeedReader reader = new MediaListFeedReader(new URL(serverUrl));
			replaceList(reader.getMediaItemList());
			
		} catch (Exception e) {
			throw new MorriganException(e);
		}
		
	};
	
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
	
	public boolean addFile (File file) throws MorriganException {
		throw new NotImplementedException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

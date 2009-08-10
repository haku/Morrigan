package net.sparktank.morrigan.model.media;

import java.io.File;

public class MediaPlaylist extends MediaList {
	
	private String filePath = null;
	
	public MediaPlaylist(String filePath) {
		super(getFilenameFromPath(filePath));
		this.filePath = filePath;
		ReloadFromFile();
	}
	
	public void ReloadFromFile () {
		// TODO write this.
		// FIXME temp test data.
		
		for (int i = 0; i < 10; i++) {
			addTrack("/path/media/track" + i);
		}
	}
	
	public void WriteToFile () {
		// TODO write this.
	}
	
	private static String getFilenameFromPath (String filePath) {
		int x = filePath.lastIndexOf(File.separator);
		if (x>0) {
			return filePath.substring(x+1);
		} else {
			return filePath;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		MediaPlaylist other = (MediaPlaylist) obj;
		if (filePath == null) {
			if (other.filePath != null) {
				return false;
			}
		} else if (!filePath.equals(other.filePath)) {
			return false;
		}
		
		return true;
	}
	
//	@Override
//	public int hashCode() {
//		return super.hashCode();
//	}


}

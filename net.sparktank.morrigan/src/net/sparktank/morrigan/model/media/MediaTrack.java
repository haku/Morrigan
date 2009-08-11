package net.sparktank.morrigan.model.media;

import java.io.File;

public class MediaTrack {
	
	private String trackFilePath = null;
	
	public MediaTrack (String filePath) {
		trackFilePath = filePath;
	}
	
	public String getFilepath () {
		return trackFilePath;
	}
	
	public String getTitle () {
		int x = trackFilePath.lastIndexOf(File.separator);
		if (x>0) {
			return trackFilePath.substring(x+1);
		} else {
			return trackFilePath;
		}
	}
	
	@Override
	public String toString () {
		return trackFilePath;
	}
	
}

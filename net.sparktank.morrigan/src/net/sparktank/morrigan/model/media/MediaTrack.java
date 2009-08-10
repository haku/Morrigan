package net.sparktank.morrigan.model.media;

public class MediaTrack {
	
	private String trackFilePath = null;
	
	public MediaTrack (String filePath) {
		trackFilePath = filePath;
	}
	
	public String getFilepath () {
		return trackFilePath;
	}
	
}

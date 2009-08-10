package net.sparktank.morrigan.model;

public class MediaLibrary extends MediaList {
	
	private String mediaDbFilePath = null;
	
	public MediaLibrary (String libraryName, String dbFilePath) {
		super(libraryName);
		mediaDbFilePath = dbFilePath;
	}
	
	public int updateLibrary () {
		
		// TODO load DB content and return status code.
		
		genTestContent();
		return 1;
	}
	
	private void genTestContent () {
		for (int i=0; i<10; i++) {
			addTrack("/media/track" + i);
		}
	}
	
}

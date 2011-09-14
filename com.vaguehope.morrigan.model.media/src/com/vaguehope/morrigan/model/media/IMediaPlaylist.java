package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

public interface IMediaPlaylist extends IMediaTrackList<IMediaTrack> {
//	- - - - - - - - - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "PLAYLIST";
	
//	- - - - - - - - - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getFilePath ();
	
	public void loadFromFile () throws MorriganException;
	public void writeToFile () throws MorriganException;
	
	public void addNewItem (String filepath);
	
//	- - - - - - - - - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
}
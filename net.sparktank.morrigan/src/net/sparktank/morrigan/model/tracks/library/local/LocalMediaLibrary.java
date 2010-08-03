package net.sparktank.morrigan.model.tracks.library.local;

import net.sparktank.morrigan.model.tracks.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.tracks.library.SqliteLayer;


public class LocalMediaLibrary extends AbstractMediaLibrary {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LIBRARY";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	//TODO rename to "LocalLibrary".
	public LocalMediaLibrary (String libraryName, SqliteLayer dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}

package net.sparktank.morrigan.model.library;


public class MediaLibrary extends AbstractLibrary {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LIBRARY";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	//TODO rename to "LocalLibrary".
	public MediaLibrary (String libraryName, SqliteLayer dbLayer) throws DbException {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}

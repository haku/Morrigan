package net.sparktank.morrigan.model.pictures.gallery;

import java.util.List;

import net.sparktank.morrigan.model.MediaSqliteLayer;
import net.sparktank.sqlitewrapper.DbException;

public class GallerySqliteLayer extends MediaSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected GallerySqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for ...
	
	// TODO
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Schema.
	
	/* - - - - - - - - - - - - - - - -
	 * tbl_...
	 */
	
	// TODO
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL statements.
	
	/* - - - - - - - - - - - - - - - -
	 * tbl_...
	 */
	
	// TODO
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Superclass methods.
	
	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = super.getTblCreateCmds();
		
		// TODO create tables here.
		
		return l;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for...
	
	// TODO
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

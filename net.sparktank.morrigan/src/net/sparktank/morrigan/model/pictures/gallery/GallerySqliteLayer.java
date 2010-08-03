package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.sqlitewrapper.DbException;
import net.sparktank.sqlitewrapper.GenericSqliteLayer;

public class GallerySqliteLayer extends GenericSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected GallerySqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected SqlCreateCmd[] getTblCreateCmds() {
		// TODO Auto-generated method stub
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

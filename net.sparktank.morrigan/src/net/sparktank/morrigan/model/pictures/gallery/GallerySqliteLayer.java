package net.sparktank.morrigan.model.pictures.gallery;

import java.util.List;

import net.sparktank.sqlitewrapper.DbException;
import net.sparktank.sqlitewrapper.GenericSqliteLayer;

public class GallerySqliteLayer extends GenericSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected GallerySqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		// TODO Auto-generated method stub
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

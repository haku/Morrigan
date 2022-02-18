package com.vaguehope.morrigan.sqlitewrapper;

public interface IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void dispose();

	String getDbFilePath();

	void commitOrRollBack() throws DbException;
	void rollback() throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
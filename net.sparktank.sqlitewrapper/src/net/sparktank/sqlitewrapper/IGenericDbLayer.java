package net.sparktank.sqlitewrapper;

public interface IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public abstract void dispose();
	
	public abstract String getDbFilePath();
	
	public abstract void commitOrRollBack() throws DbException;
	public abstract void rollback() throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
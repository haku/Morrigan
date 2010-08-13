package net.sparktank.sqlitewrapper;

public interface IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public abstract void dispose() throws DbException;
	
	public abstract String getDbFilePath();
	
	public abstract void setAutoCommit(boolean b) throws DbException;
	public abstract void commit() throws DbException;
	public abstract void rollback() throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
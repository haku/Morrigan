package net.sparktank.morrigan.model;

public interface IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public long getDbRowId();
	public boolean setDbRowId(long dbRowId);
	
	public String getRemoteLocation();
	public boolean setRemoteLocation(String remoteLocation);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

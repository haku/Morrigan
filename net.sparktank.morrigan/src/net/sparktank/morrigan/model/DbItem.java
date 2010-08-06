package net.sparktank.morrigan.model;

import net.sparktank.morrigan.helpers.EqualHelper;

public class DbItem implements IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long dbRowId;
	private String remoteLocation;
	
	@Override
	public long getDbRowId() {
		return this.dbRowId;
	}
	@Override
	public boolean setDbRowId(long dbRowId) {
		/* Sqlite ROWID starts at 1, so if something tries to set it
		 * less than this, don't let them clear it.
		 * This is most likely when fetching a remote list over HTTP.
		 */
		if (dbRowId > 0 && this.dbRowId != dbRowId) {
			this.dbRowId = dbRowId;
			return true;
		}
		return false;
	}
	
	@Override
	public String getRemoteLocation() {
		return this.remoteLocation;
	}
	@Override
	public boolean setRemoteLocation(String remoteLocation) {
		if (!EqualHelper.areEqual(this.remoteLocation, remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean set (IDbItem item) {
		return this.setDbRowId(item.getDbRowId())
		|| this.setRemoteLocation(item.getRemoteLocation());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

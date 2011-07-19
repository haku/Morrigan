package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.helper.EqualHelper;

final class LocalMixedMediaDbConfig {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String filePath;
	private final String filter;
	private final int hash;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	LocalMixedMediaDbConfig (String filePath, String filterString) {
		this.filePath = filePath;
		this.filter = filterString;
		this.hash = (filePath + filterString).hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public String getFilter() {
		return this.filter;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Important for HashMap to work.
	
	@Override
	public boolean equals (Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof LocalMixedMediaDbConfig) ) return false;
		LocalMixedMediaDbConfig that = (LocalMixedMediaDbConfig)aThat;
		
		return EqualHelper.areEqual(getFilePath(), that.getFilePath())
			&& EqualHelper.areEqual(getFilter(), that.getFilter())
			;
	}
	
	@Override
	public int hashCode() {
		return this.hash;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
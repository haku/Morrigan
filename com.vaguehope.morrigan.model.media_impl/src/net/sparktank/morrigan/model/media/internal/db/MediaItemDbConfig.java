package net.sparktank.morrigan.model.media.internal.db;

import java.io.File;

import net.sparktank.morrigan.model.helper.EqualHelper;

public final class MediaItemDbConfig {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String filePath;
	private final String filter;
	private final int hash;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaItemDbConfig (String serial) {
		this(fileFromSerial(serial), filterFromSerial(serial));
	}
	
	public MediaItemDbConfig (String filePath, String filterString) {
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
	
	public String getSerial () {
		return getFilePath() + File.pathSeparator + (getFilter() == null ? "" : getFilter());
	}
	
	private static String fileFromSerial (String serial) {
		return serial.substring(0, serial.indexOf(File.pathSeparator));
	}
	
	private static String filterFromSerial (String serial) {
		int x = serial.indexOf(File.pathSeparator);
		return x < serial.length() - 1 ? serial.substring(x + 1) : null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Important for HashMap to work.
	
	@Override
	public boolean equals (Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof MediaItemDbConfig) ) return false;
		MediaItemDbConfig that = (MediaItemDbConfig)aThat;
		
		return EqualHelper.areEqual(getFilePath(), that.getFilePath())
			&& EqualHelper.areEqual(getFilter(), that.getFilter())
			;
	}
	
	@Override
	public int hashCode() {
		return this.hash;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[path=" + getFilePath() + " filter=" + (getFilter() == null ? "" : getFilter()) + " serial=" + getSerial() + "]";
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static void main (String[] args) {
		MediaItemDbConfig a = new MediaItemDbConfig("/path/var/foo", ":;abc");
		System.out.println(a);
		MediaItemDbConfig b = new MediaItemDbConfig(a.getSerial());
		System.out.println(b);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
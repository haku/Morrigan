package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.util.Objects;

public final class MediaDbConfig {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final String filePath;
	private final String filter;
	private final int hash; // cache.

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MediaDbConfig (String serial) {
		this(fileFromSerial(serial), filterFromSerial(serial));
	}

	public MediaDbConfig (String filePath, String filterString) {
		this.filePath = filePath;
		this.filter = filterString;
		this.hash = (filePath + filterString).hashCode();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public String getFilePath () {
		return this.filePath;
	}

	public String getFilter () {
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
		if (aThat == null) return false;
		if (this == aThat) return true;
		if (!(aThat instanceof MediaDbConfig)) return false;
		MediaDbConfig that = (MediaDbConfig) aThat;

		return Objects.equals(getFilePath(), that.getFilePath())
				&& Objects.equals(getFilter(), that.getFilter());
	}

	@Override
	public int hashCode () {
		return this.hash;
	}

	@Override
	public String toString () {
		return this.getClass().getSimpleName() + "[path=" + getFilePath() + " filter=" + (getFilter() == null ? "" : getFilter()) + " serial=" + getSerial() + "]";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void main (String[] args) {
		MediaDbConfig a = new MediaDbConfig("/path/var/foo", ":;abc");
		System.out.println(a);
		MediaDbConfig b = new MediaDbConfig(a.getSerial());
		System.out.println(b);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

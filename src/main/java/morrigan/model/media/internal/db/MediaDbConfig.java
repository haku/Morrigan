package morrigan.model.media.internal.db;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import morrigan.model.media.ListRef;

public final class MediaDbConfig {

	private final String filePath;
	private final String filter;
	private final int hash; // cache.

	public MediaDbConfig (String filePath, String filterString) {
		this.filePath = filePath;
		this.filter = filterString;
		this.hash = (filePath + filterString).hashCode();
	}

	public String getFilePath () {
		return this.filePath;
	}

	public String getFilter () {
		return this.filter;
	}

	public ListRef toListRef() {
		final String name = LocalMediaDbHelper.listIdForFilepath(this.filePath);
		if (StringUtils.isNotBlank(this.filter)) return ListRef.forLocalSearch(name, this.filter);
		return ListRef.forLocal(name);
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
		return this.getClass().getSimpleName() + "[path=" + getFilePath() + " filter=" + getFilter() + "]";
	}

}

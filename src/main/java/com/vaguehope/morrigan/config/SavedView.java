package com.vaguehope.morrigan.config;

import com.vaguehope.morrigan.util.Objs;

public class SavedView {

	private final String name;
	private final String dbmid;
	private final String query;

	public SavedView(final String name, final String dbmid, final String query) {
		this.name = name;
		this.dbmid = dbmid;
		this.query = query;
	}

	public String getName() {
		return this.name;
	}

	public String getDbmid() {
		return this.dbmid;
	}

	public String getQuery() {
		return this.query;
	}

	@Override
	public int hashCode() {
		return Objs.hash(this.name, this.dbmid, this.query);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof SavedView)) return false;
		final SavedView that = (SavedView) obj;

		return Objs.equals(this.name, that.name)
				&& Objs.equals(this.dbmid, that.dbmid)
				&& Objs.equals(this.query, that.query);
	}

}

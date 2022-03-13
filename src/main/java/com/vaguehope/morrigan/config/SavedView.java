package com.vaguehope.morrigan.config;

public class SavedView {

	private String name;
	private String dbmid;
	private String query;

	public SavedView(String name, String dbmid, String query) {
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

}

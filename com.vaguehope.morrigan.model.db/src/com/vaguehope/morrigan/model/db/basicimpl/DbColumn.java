package com.vaguehope.morrigan.model.db.basicimpl;

import com.vaguehope.morrigan.model.db.IDbColumn;

public class DbColumn implements IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final String name;
	private final String defaultValue;
	private final String sqlType;
	private final String humanName;
	private final String sortOpts;

	public DbColumn (String name, String humanName, String sqlType, String defaultValue) {
		this(name, humanName, sqlType, defaultValue, null);
	}

	public DbColumn (String name, String humanName, String sqlType, String defaultValue, String sortOpts) {
		this.name = name;
		this.humanName = humanName;
		this.sqlType = sqlType;
		this.defaultValue = defaultValue;
		this.sortOpts = sortOpts;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public String getSqlType() {
		return this.sqlType;
	}

	@Override
	public String getHumanName() {
		return this.humanName;
	}

	@Override
	public String getSortOpts() {
		return this.sortOpts;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

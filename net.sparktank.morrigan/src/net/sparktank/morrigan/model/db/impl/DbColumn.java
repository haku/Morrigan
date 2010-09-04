package net.sparktank.morrigan.model.db.impl;

import net.sparktank.morrigan.model.db.interfaces.IDbColumn;

public class DbColumn implements IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String name;
	private String defaultValue;
	private String sqlType;
	private final String humanName;
	private final String sortOpts;
	
	public DbColumn (String name, String humanName, String sqlType, String defaultValue, String sortOpts) {
		this.setName(name);
		this.humanName = humanName;
		this.setSqlType(sqlType);
		this.setDefaultValue(defaultValue);
		this.sortOpts = sortOpts;
	}
	
	public DbColumn (String name, String humanName, String sqlType, String defaultValue) {
		this.setName(name);
		this.humanName = humanName;
		this.setSqlType(sqlType);
		this.setDefaultValue(defaultValue);
		this.sortOpts = null;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	@Override
	public String getDefaultValue() {
		return this.defaultValue;
	}
	
	@Override
	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
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

package net.sparktank.morrigan.model;

public class DbColumn {
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
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return this.name;
	}
	
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public String getDefaultValue() {
		return this.defaultValue;
	}
	
	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}
	public String getSqlType() {
		return this.sqlType;
	}
	
	public String getHumanName() {
		return this.humanName;
	}
	
	public String getSortOpts() {
		return this.sortOpts;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

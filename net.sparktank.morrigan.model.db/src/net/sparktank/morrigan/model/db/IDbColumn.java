package net.sparktank.morrigan.model.db;

public interface IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public abstract String getName();
	public abstract void setName(String name);
	
	public abstract String getDefaultValue();
	public abstract void setDefaultValue(String defaultValue);
	
	public abstract String getSqlType();
	public abstract void setSqlType(String sqlType);
	
	public abstract String getSortOpts();
	public abstract String getHumanName();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
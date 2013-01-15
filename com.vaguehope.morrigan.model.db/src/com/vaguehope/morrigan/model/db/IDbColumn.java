package com.vaguehope.morrigan.model.db;

public interface IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	String getName();
	void setName(String name);

	String getDefaultValue();
	void setDefaultValue(String defaultValue);

	String getSqlType();
	void setSqlType(String sqlType);

	String getSortOpts();
	String getHumanName();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
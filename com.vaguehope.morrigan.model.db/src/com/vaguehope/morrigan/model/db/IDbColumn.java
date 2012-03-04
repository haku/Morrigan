package com.vaguehope.morrigan.model.db;

public interface IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	abstract String getName();
	abstract void setName(String name);

	abstract String getDefaultValue();
	abstract void setDefaultValue(String defaultValue);

	abstract String getSqlType();
	abstract void setSqlType(String sqlType);

	abstract String getSortOpts();
	abstract String getHumanName();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
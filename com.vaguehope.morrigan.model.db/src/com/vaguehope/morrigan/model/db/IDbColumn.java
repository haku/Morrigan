package com.vaguehope.morrigan.model.db;

public interface IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	String getName();
	String getDefaultValue();
	String getSqlType();
	String getSortOpts();
	String getHumanName();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
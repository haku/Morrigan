package com.vaguehope.morrigan.model.db;

//TODO remove 'I'.
public interface IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	String getName();
	String getDefaultValue();
	String getSqlType();
	String getSortOpts();
	String getHumanName();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
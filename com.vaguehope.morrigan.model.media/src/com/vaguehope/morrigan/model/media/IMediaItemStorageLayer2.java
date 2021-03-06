package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.basicimpl.DbColumn;

public interface IMediaItemStorageLayer2<T extends IMediaItem> extends IMediaItemStorageLayer<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/*
	 * TODO this is DB-specific.  Should move it somewhere better...
	 */

	IDbColumn SQL_TBL_MEDIAFILES_COL_ROWID     = new DbColumn("ROWID", null, null, null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_FILE      = new DbColumn("sfile",     "file path",     "VARCHAR(1000) not null collate nocase primary key", "?", " collate nocase");
	IDbColumn SQL_TBL_MEDIAFILES_COL_MD5       = new DbColumn("md5",       "MD5",           "BLOB",     null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_DADDED    = new DbColumn("dadded",    "date added",    "DATETIME", "?");
	IDbColumn SQL_TBL_MEDIAFILES_COL_DMODIFIED = new DbColumn("dmodified", "date modified", "DATETIME", "?");
	IDbColumn SQL_TBL_MEDIAFILES_COL_ENABLED   = new DbColumn("benabled",  null,            "INT(1)",   "1");
	IDbColumn SQL_TBL_MEDIAFILES_COL_MISSING   = new DbColumn("bmissing",  null,            "INT(1)",   "0");
	IDbColumn SQL_TBL_MEDIAFILES_COL_REMLOC    = new DbColumn("sremloc",   null,            "VARCHAR(1000) NOT NULL", "''");

	IDbColumn[] SQL_TBL_MEDIAFILES_COLS = new IDbColumn[] {
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_MD5,
		SQL_TBL_MEDIAFILES_COL_DADDED,
		SQL_TBL_MEDIAFILES_COL_DMODIFIED,
		SQL_TBL_MEDIAFILES_COL_ENABLED,
		SQL_TBL_MEDIAFILES_COL_MISSING,
		SQL_TBL_MEDIAFILES_COL_REMLOC
		};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

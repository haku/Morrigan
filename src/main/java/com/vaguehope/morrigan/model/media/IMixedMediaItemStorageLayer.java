package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.basicimpl.DbColumn;

public interface IMixedMediaItemStorageLayer extends IMediaItemStorageLayer<IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	IDbColumn SQL_TBL_MEDIAFILES_COL_ID        = new DbColumn("id",        "id",            "INTEGER PRIMARY KEY AUTOINCREMENT", null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_TYPE      = new DbColumn("type",      "type",          "INT",      "?");
	IDbColumn SQL_TBL_MEDIAFILES_COL_FILE      = new DbColumn("file",      "file path",     "VARCHAR(1000) NOT NULL COLLATE NOCASE UNIQUE", "?", " COLLATE NOCASE");
	IDbColumn SQL_TBL_MEDIAFILES_COL_MD5       = new DbColumn("md5",       "MD5",           "BLOB",     null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_DADDED    = new DbColumn("added",     "date added",    "DATETIME", "?");
	IDbColumn SQL_TBL_MEDIAFILES_COL_DMODIFIED = new DbColumn("modified",  "date modified", "DATETIME", "?");
	IDbColumn SQL_TBL_MEDIAFILES_COL_ENABLED   = new DbColumn("enabled",   null,            "INT(1)",   "1");
	IDbColumn SQL_TBL_MEDIAFILES_COL_ENABLEDMODIFIED = new DbColumn("enabledmodified",   null,            "DATETIME",   null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_MISSING   = new DbColumn("missing",   null,            "INT(1)",   "0");
	IDbColumn SQL_TBL_MEDIAFILES_COL_REMLOC    = new DbColumn("remloc",    null,            "VARCHAR(1000)", "''");

	IDbColumn SQL_TBL_MEDIAFILES_COL_STARTCNT =  new DbColumn("startcnt", "start count", "INT(6)",   "0");
	IDbColumn SQL_TBL_MEDIAFILES_COL_ENDCNT =    new DbColumn("endcnt",   "end count",   "INT(6)",   "0");
	IDbColumn SQL_TBL_MEDIAFILES_COL_DLASTPLAY = new DbColumn("lastplay", "last played", "DATETIME", null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_DURATION =  new DbColumn("duration", "duration",    "INT(6)",   "-1");

	IDbColumn SQL_TBL_MEDIAFILES_COL_WIDTH =  new DbColumn("width",  "width",  "INT(6)",   "0");
	IDbColumn SQL_TBL_MEDIAFILES_COL_HEIGHT = new DbColumn("height", "height", "INT(6)",   "0");

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.basicimpl.DbColumn;

public interface IMixedMediaItemStorageLayer extends IMediaItemStorageLayer {

	IDbColumn SQL_TBL_MEDIAFILES_COL_ID        = new DbColumn("id",        "id",            "INTEGER PRIMARY KEY AUTOINCREMENT", null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_TYPE      = new DbColumn("type",      "type",          "INT",      "?");
	IDbColumn SQL_TBL_MEDIAFILES_COL_MIMETYPE  = new DbColumn("mimetype",  "mimetype",      "STRING",   null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_FILE      = new DbColumn("file",      "file path",     "VARCHAR(1000) NOT NULL COLLATE NOCASE UNIQUE", "?", " COLLATE NOCASE");
	IDbColumn SQL_TBL_MEDIAFILES_COL_MD5       = new DbColumn("md5",       "MD5",           "BLOB",     null);
	IDbColumn SQL_TBL_MEDIAFILES_COL_SHA1      = new DbColumn("sha1",      "SHA1",          "BLOB",     null);
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

	// short term for migrations.
	public static SortColumn parseOldColName(String sortcol) {
		if ("file".equals(sortcol)) return SortColumn.FILE_PATH;
		else if ("added".equals(sortcol)) return SortColumn.DATE_ADDED;
		else if ("duration".equals(sortcol)) return SortColumn.DURATION;
		else if ("lastplay".equals(sortcol)) return SortColumn.DATE_LAST_PLAYED;
		else if ("startcnt".equals(sortcol)) return SortColumn.START_COUNT;
		else if ("endcnt".equals(sortcol)) return SortColumn.END_COUNT;
		else return null;
	}

	public static IDbColumn columnFromEnum(final SortColumn col) {
		switch (col) {
		case FILE_PATH:
			return SQL_TBL_MEDIAFILES_COL_FILE;
		case DATE_ADDED:
			return SQL_TBL_MEDIAFILES_COL_DADDED;
		case DATE_LAST_PLAYED:
			return SQL_TBL_MEDIAFILES_COL_DLASTPLAY;
		case START_COUNT:
			return SQL_TBL_MEDIAFILES_COL_STARTCNT;
		case END_COUNT:
			return SQL_TBL_MEDIAFILES_COL_ENDCNT;
		case DURATION:
			return SQL_TBL_MEDIAFILES_COL_DURATION;
		default:
			throw new IllegalArgumentException("No column for: " + col);
		}
	}

}

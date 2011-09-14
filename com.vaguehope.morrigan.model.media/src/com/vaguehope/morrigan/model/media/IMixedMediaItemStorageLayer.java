package com.vaguehope.morrigan.model.media;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.basicimpl.DbColumn;

public interface IMixedMediaItemStorageLayer extends IMediaItemStorageLayer<IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_ROWID     = new DbColumn("ROWID", null, null, null);
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_TYPE      = new DbColumn("type",       "type",          "INT",      "?");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_FILE      = new DbColumn("sfile",      "file path",     "VARCHAR(1000) not null collate nocase primary key", "?", " collate nocase");
//	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_HASHCODE  = new DbColumn("lmd5",       "hashcode",      "BIGINT",   null);
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_MD5       = new DbColumn("md5",        "MD5",           "BLOB",     null);
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_DADDED    = new DbColumn("dadded",     "date added",    "DATETIME", "?");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_DMODIFIED = new DbColumn("dmodified",  "date modified", "DATETIME", "?");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_ENABLED   = new DbColumn("benabled",   null,            "INT(1)",   "1");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_MISSING   = new DbColumn("bmissing",   null,            "INT(1)",   "0");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_REMLOC    = new DbColumn("sremloc",    null,            "VARCHAR(1000) NOT NULL", "''");
	
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_STARTCNT =  new DbColumn("lstartcnt", "start count", "INT(6)",   "0");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_ENDCNT =    new DbColumn("lendcnt",   "end count",   "INT(6)",   "0");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_DLASTPLAY = new DbColumn("dlastplay", "last played", "DATETIME", null);
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_DURATION =  new DbColumn("lduration", "duration",    "INT(6)",   "-1");
	
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_WIDTH =  new DbColumn("lwidth",  "width",  "INT(6)",   "0");
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_HEIGHT = new DbColumn("lheight", "height", "INT(6)",   "0");
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

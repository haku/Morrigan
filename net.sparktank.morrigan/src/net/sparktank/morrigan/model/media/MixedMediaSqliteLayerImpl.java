package net.sparktank.morrigan.model.media;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.model.DbColumn;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaSqliteLayer;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.media.IMixedMediaSqlLayer.MediaType;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerImpl extends MediaSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MixedMediaSqliteLayerImpl (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL for defining tables.
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	SQL for MediaItem.
	
	public static final String SQL_TBL_MEDIAFILES_NAME = "tbl_mediafiles";
	
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_ROWID     = new DbColumn("m.ROWID", null, null, null);
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_TYPE      = new DbColumn("type",       "type",          "INT",      "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_TYPEROWID = new DbColumn("type_rowid", "type_rowid",          "INT",      "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_FILE      = new DbColumn("sfile",      "file path",     "VARCHAR(1000) not null collate nocase primary key", "?", " collate nocase");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_HASHCODE  = new DbColumn("lmd5",       "hashcode",      "BIGINT",   null);
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DADDED    = new DbColumn("dadded",     "date added",    "DATETIME", "?");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DMODIFIED = new DbColumn("dmodified",  "date modified", "DATETIME", "?");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_ENABLED   = new DbColumn("benabled",   null,            "INT(1)",   "1");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_MISSING   = new DbColumn("bmissing",   null,            "INT(1)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_REMLOC    = new DbColumn("sremloc",    null,            "VARCHAR(1000) NOT NULL", "''");
	
	public static final DbColumn[] SQL_TBL_MEDIAFILES_COLS = new DbColumn[] {
		SQL_TBL_MEDIAFILES_COL_TYPE,
		SQL_TBL_MEDIAFILES_COL_TYPEROWID,
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_HASHCODE,
		SQL_TBL_MEDIAFILES_COL_DADDED,
		SQL_TBL_MEDIAFILES_COL_DMODIFIED,
		SQL_TBL_MEDIAFILES_COL_ENABLED,
		SQL_TBL_MEDIAFILES_COL_MISSING,
		SQL_TBL_MEDIAFILES_COL_REMLOC
		};
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	SQL for MediaTrack.
	
	public static final String SQL_TBL_MEDIATRACKS_NAME = "tbl_mediatracks";
	
	public static final DbColumn SQL_TBL_MEDIATRACKS_COL_ROWID =     new DbColumn("t.ROWID", null, null, null);
	public static final DbColumn SQL_TBL_MEDIATRACKS_COL_STARTCNT =  new DbColumn("lstartcnt", "start count", "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIATRACKS_COL_ENDCNT =    new DbColumn("lendcnt",   "end count",   "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIATRACKS_COL_DLASTPLAY = new DbColumn("dlastplay", "last played", "DATETIME", null);
	public static final DbColumn SQL_TBL_MEDIATRACKS_COL_DURATION =  new DbColumn("lduration", "duration",    "INT(6)",   "-1");
	
	public static final DbColumn[] SQL_TBL_MEDIATRACKS_COLS = new DbColumn[] {
		SQL_TBL_MEDIATRACKS_COL_STARTCNT,
		SQL_TBL_MEDIATRACKS_COL_ENDCNT,
		SQL_TBL_MEDIATRACKS_COL_DLASTPLAY,
		SQL_TBL_MEDIATRACKS_COL_DURATION,
		};
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	SQL for MediaPicture.
	
	public static final String SQL_TBL_MEDIAPICS_NAME = "tbl_mediapics";
	
	public static final DbColumn SQL_TBL_MEDIAPICS_COL_ROWID =  new DbColumn("p.ROWID", null, null, null);
	public static final DbColumn SQL_TBL_MEDIAPICS_COL_WIDTH =  new DbColumn("lwidth",  "width",  "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAPICS_COL_HEIGHT = new DbColumn("lheight", "height", "INT(6)",   "0");
	
	public static final DbColumn[] SQL_TBL_MEDIAPICS_COLS = new DbColumn[] {
		SQL_TBL_MEDIAPICS_COL_WIDTH,
		SQL_TBL_MEDIAPICS_COL_HEIGHT,
		};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL queries.
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Fragments.
	
	private static final String _SQL_MEDIAFILES_SELECT =
		"SELECT"
		+ " m.ROWID, type, type_rowid, sfile, lmd5, dadded, dmodified, benabled, bmissing, sremloc"
		+ ",lstartcnt,lendcnt,dlastplay,lduration"
		+ ",lwidth,lheight"
		+ " FROM tbl_mediafiles AS m";
	
	private static final String _SQL_MEDIA_JOINTRACKS =
		" LEFT OUTER JOIN tbl_mediatracks AS t ON m.type_rowid = t.ROWID";
	
	private static final String _SQL_MEDIA_JOINPICS =
		" LEFT OUTER JOIN tbl_mediapics AS p ON m.type_rowid = p.ROWID";
	
	private static final String _SQL_MEDIAFILES_WHERENOTMISSING =
		" WHERE (bmissing<>1 OR bmissing is NULL)";
	
	private static final String _SQL_ORDERBYREPLACE =
		" ORDER BY {COL} {DIR};";
	
	private static final String _SQL_MEDIAFILES_WHEREORDERSEARCH = 
		" WHERE sfile LIKE ? ESCAPE ?"
		+ " AND (bmissing<>1 OR bmissing is NULL) AND (benabled<>0 OR benabled is NULL)"
		+ " ORDER BY dlastplay DESC, lendcnt DESC, lstartcnt DESC, sfile COLLATE NOCASE ASC;";
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	MediaItem Queries.
	
	private static final String SQL_MEDIAFILES_Q_ALL =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINTRACKS + _SQL_MEDIA_JOINPICS
		+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAFILES_Q_NOTMISSING =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINTRACKS + _SQL_MEDIA_JOINPICS
    	+ _SQL_MEDIAFILES_WHERENOTMISSING
    	+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAFILES_Q_SIMPLESEARCH =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINTRACKS + _SQL_MEDIA_JOINPICS
		+ _SQL_MEDIAFILES_WHEREORDERSEARCH;

//	-  -  -  -  -  -  -  -  -  -  -  -
//	MediaTrack Queries.
	
	private static final String SQL_MEDIATRACKS_Q_ALL =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINTRACKS
		+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIATRACKS_Q_NOTMISSING =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINTRACKS
    	+ _SQL_MEDIAFILES_WHERENOTMISSING
    	+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIATRACKS_Q_SIMPLESEARCH =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINTRACKS
		+ _SQL_MEDIAFILES_WHEREORDERSEARCH;
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	MediaPic Queries.
	
	private static final String SQL_MEDIAPICS_Q_ALL =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINPICS
		+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAPICS_Q_NOTMISSING =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINPICS
    	+ _SQL_MEDIAFILES_WHERENOTMISSING
    	+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAPICS_Q_SIMPLESEARCH =
		_SQL_MEDIAFILES_SELECT + _SQL_MEDIA_JOINPICS
		+ _SQL_MEDIAFILES_WHEREORDERSEARCH;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create statements.
	
	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = super.getTblCreateCmds();
		
		l.add(SqliteHelper.generateSql_Create(SQL_TBL_MEDIAFILES_NAME, SQL_TBL_MEDIAFILES_COLS));
		l.add(SqliteHelper.generateSql_Create(SQL_TBL_MEDIATRACKS_NAME, SQL_TBL_MEDIATRACKS_COLS));
		l.add(SqliteHelper.generateSql_Create(SQL_TBL_MEDIAPICS_NAME, SQL_TBL_MEDIAPICS_COLS));
		
		return l;
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem getters.
	
	protected List<MediaItem> local_updateListOfAllMedia (List<MediaItem> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(SQL_MEDIAFILES_Q_ALL, SQL_MEDIAFILES_Q_NOTMISSING, hideMissing, sort, direction);
		ResultSet rs;
		
		List<MediaItem> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			rs = ps.executeQuery();
			try {
				ret = local_parseAndUpdateFromRecordSet(list, rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	protected List<MediaItem> local_getAllMedia (DbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(SQL_MEDIAFILES_Q_ALL, SQL_MEDIAFILES_Q_NOTMISSING, hideMissing, sort, direction);
		ResultSet rs;
		
		List<MediaItem> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	protected List<MediaItem> local_simpleSearch (String term, String esc, int maxResults) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<MediaItem> ret;
		
		ps = getDbCon().prepareStatement(SQL_MEDIAFILES_Q_SIMPLESEARCH);
		try {
			ps.setString(1, "%" + term + "%");
			ps.setString(2, esc);
			
			if (maxResults > 0) {
				ps.setMaxRows(maxResults);
			}
			
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack getters.
	
//	TODO
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaPic getters.
	
//	TODO
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media setters.
	
//	TODO
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Static processing methods.
	
	static private String local_getAllMediaSql (String sqlAll, String sqlNotMissing, boolean hideMissing, DbColumn sort, SortDirection direction) {
		String sql;
		
		if (hideMissing) {
			sql = sqlNotMissing;
		} else {
			sql = sqlAll;
		}
		
		switch (direction) {
			case ASC:
				sql = sql.replace("{DIR}", "ASC");
				break;
				
			case DESC:
				sql = sql.replace("{DIR}", "DESC");
				break;
				
			default:
				throw new IllegalArgumentException();
				
		}
		
		String sortTerm = sort.getName();
		if (sort.getSortOpts() != null) {
			sortTerm = sortTerm.concat(sort.getSortOpts());
		}
		sql = sql.replace("{COL}", sortTerm);
		
		return sql;
	}
	
	static private List<MediaItem> local_parseAndUpdateFromRecordSet (List<MediaItem> list, ResultSet rs) throws SQLException {
		List<MediaItem> finalList = new ArrayList<MediaItem>();
		
		// Build a HashMap of existing items to make lookup a lot faster.
		Map<String, MediaItem> keepMap = new HashMap<String, MediaItem>(list.size());
		for (MediaItem e : list) {
			keepMap.put(e.getFilepath(), e);
		}
		
		/* Extract entry from DB.  Compare it to existing entries and
		 * create new list as we go. 
		 */
		while (rs.next()) {
			MediaItem newItem = createMediaItem(rs);
			MediaItem oldItem = keepMap.get(newItem.getFilepath());
			if (oldItem != null) {
				oldItem.setFromMediaItem(newItem);
				finalList.add(oldItem);
			} else {
				finalList.add(newItem);
			}
		}
		
		return finalList;
	}
	
	static private List<MediaItem> local_parseRecordSet (ResultSet rs) throws SQLException {
		List<MediaItem> ret = new ArrayList<MediaItem>();
		
		while (rs.next()) {
			MediaItem mi = createMediaItem(rs);
			ret.add(mi);
		}
		
		return ret;
	}
	
	static protected MediaItem createMediaItem (ResultSet rs) throws SQLException {
		int i = rs.getInt(SQL_TBL_MEDIAFILES_COL_TYPE.getName());
		MediaType t = MediaType.parseInt(i);
		MediaItem mi;
		
		switch (t) {
			case TRACK:
				mi = createMediaTrack(rs);
				break;
			
			case PICTURE:
				mi = createMediaPic(rs);
				break;
			
			case UNKNOWN:
			default:
				throw new IllegalArgumentException("Encountered media with type UNKNOWN.");
			
		}
		
		return mi;
	}
	
	static protected MediaTrack createMediaTrack (ResultSet rs) throws SQLException {
		MediaTrack mt = new MediaTrack();
		createMediaItem(mt, rs);
		
		mt.setStartCount(rs.getLong(SQL_TBL_MEDIATRACKS_COL_STARTCNT.getName()));
		mt.setEndCount(rs.getLong(SQL_TBL_MEDIATRACKS_COL_ENDCNT.getName()));
		mt.setDuration(rs.getInt(SQL_TBL_MEDIATRACKS_COL_DURATION.getName()));
		mt.setDateLastPlayed(SqliteHelper.readDate(rs, SQL_TBL_MEDIATRACKS_COL_DLASTPLAY.getName()));
		
		return mt;
	}
	
	static protected MediaPicture createMediaPic (ResultSet rs) throws SQLException {
		MediaPicture mp = new MediaPicture();
		createMediaItem(mp, rs);
		
		mp.setWidth(rs.getInt(SQL_TBL_MEDIAPICS_COL_WIDTH.getName()));
		mp.setHeight(rs.getInt(SQL_TBL_MEDIAPICS_COL_HEIGHT.getName()));
		
		return mp;
	}
	
	static protected MediaItem createMediaItem (MediaItem mi, ResultSet rs) throws SQLException {
		mi.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE.getName()));
		mi.setDateAdded(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED.getName()));
		mi.setHashcode(rs.getLong(SQL_TBL_MEDIAFILES_COL_HASHCODE.getName()));
		mi.setDateLastModified(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED.getName()));
		mi.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED.getName()) != 0); // default to true.
		mi.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING.getName()) == 1); // default to false.
		mi.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID.getName()));
		mi.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC.getName()));
		
		return mi;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

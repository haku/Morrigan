package com.vaguehope.morrigan.model.media.internal.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.sqlitewrapper.GenericSqliteLayer.SqlCreateCmd;

/*
 * Totally generic SQLite helper methods.
 *
 * TODO put somewhere more generic?
 *
 */
public final class SqliteHelper {

	private SqliteHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static SqlCreateCmd generateSql_Create (final String tblName, final IDbColumn[] columns) {
		return generateSql_Create(tblName, Arrays.asList(columns));
	}

	public static SqlCreateCmd generateSql_Create (final String tblName, final List<IDbColumn> columns) {
		StringBuilder sbExists = new StringBuilder();
		StringBuilder sbCreate = new StringBuilder();

		sbExists.append("SELECT name FROM sqlite_master WHERE name='");
		sbExists.append(tblName);
		sbExists.append("';");

		sbCreate.append("CREATE TABLE ");
		sbCreate.append(tblName);
		sbCreate.append("(");

		boolean first = true;
		for (IDbColumn c : columns) {
			if (first) {
				first = false;
			}
			else {
				sbCreate.append(",");
			}
			sbCreate.append(c.getName());
			sbCreate.append(" ");
			sbCreate.append(c.getSqlType());
		}

		sbCreate.append(");");

		return new SqlCreateCmd(sbExists.toString(), sbCreate.toString());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static ThreadLocal<SimpleDateFormat> sqlDate = new ThreadLocal<>() {
		@Override
		protected SimpleDateFormat initialValue () {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};

	/**
	 * This method will read the date from the DB weather it was stored as a
	 * number or a string. Morrigan uses the number method, but terra used
	 * strings. Using this method allows for backward compatibility.
	 */
	@Deprecated
	public static Date readDate (final ResultSet rs, final String column) throws SQLException {
		java.sql.Date date = rs.getDate(column);
		if (date != null) {
			long time = date.getTime();
			if (time > 100000) { // If the date was stored old-style, we get back the year :S.
				return new Date(time);
			}

			String s = rs.getString(column);
			try {
				Date d = sqlDate.get().parse(s);
				return d;
			} catch (Exception e) {/*Can't really do anything with this error anyway.*/}
		}

		return null;
	}

	/**
	 * This method will read the date from the DB weather it was stored as a
	 * number or a string. Morrigan uses the number method, but terra used
	 * strings. Using this method allows for backward compatibility.
	 */
	public static Date readDate (final ResultSet rs, final int column) throws SQLException {
		java.sql.Date date = rs.getDate(column);
		if (date != null) {
			long time = date.getTime();
			if (time > 100000) { // If the date was stored old-style, we get back the year :S.
				return new Date(time);
			}

			String s = rs.getString(column);
			try {
				Date d = sqlDate.get().parse(s);
				return d;
			} catch (Exception e) {/*Can't really do anything with this error anyway.*/}
		}

		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * This pairs with escapeSearch().
	 */
	public static final String SEARCH_ESC = "\\";

	/**
	 * This pairs with SEARCH_ESC.
	 */
	public static String escapeSearch (final String term) {
		String q = term;
		q = q.replace("\\", "\\\\");
		q = q.replace("%", "\\%");
		q = q.replace("_", "\\_");
		q = q.replace("*", "%");
		return q;
	}

}

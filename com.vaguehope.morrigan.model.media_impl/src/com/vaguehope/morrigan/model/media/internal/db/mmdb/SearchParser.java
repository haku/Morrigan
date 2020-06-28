package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.internal.db.SqliteHelper;

class SearchParser {

	private static final int MAX_SEARCH_TERMS = 10;

	private static final Pattern SEARCH_TERM_FINDER = Pattern.compile(
			"("
			+ "^\\("
			+ "|"
			+ "[\\s　]\\("
			+ "|"
			+ "\\)$"
			+ "|"
			+ "\\)[\\s　]"
			+ "|"
			+ "[^\\s　]*\"([^\"\\\\]*(\\\\[^\"])*(\\\\\")?)+\"([^\\s　\\)]|(\\)[^\\s　]))*"
			+ "|"
			+ "[^\\s　]*'([^'\\\\]*(\\\\[^'])*(\\\\')?)+'([^\\s　\\)]|(\\)[^\\s　]))*"
			+ "|"
			+ "([^\\s　\\)]|(\\)[^\\s　]))+"
			+ ")");

	private static final Pattern PAIRED_QUOTES_FINDER = Pattern.compile(
			"([^'\"\\\\]*)"
			+ "(?:"
			+ "(\")((?:[^\"\\\\]*(?:\\\\[^\"])*(?:\\\\\")?)+)\""
			+ "|"
			+ "(')((?:[^'\\\\]*(?:\\\\[^'])*(?:\\\\')?)+)'"
			+ ")");

	private static final String _SQL_MEDIAFILES_SELECT =
			"SELECT id,file,type,md5,added,modified,enabled,enabledmodified,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
					+ " FROM tbl_mediafiles";

	private static final String _SQL_WHERE = " WHERE";
	private static final String _SQL_AND = " AND";
	private static final String _SQL_OR = " OR";

	private static final String _SQL_MEDIAFILES_WHERTYPE =
			" type=?";

	private static final String _SQL_MEDIAFILES_WHERES_FILE =
			" (file LIKE ? ESCAPE ?)";

	private static final String _SQL_MEDIAFILES_WHERES_NOT_FILE =
			" NOT " + _SQL_MEDIAFILES_WHERES_FILE;

	private static final String _SQL_MEDIAFILES_WHERES_TAG =
			" (id IN (SELECT mf_id FROM tbl_tags WHERE tag LIKE ? ESCAPE ? AND (deleted IS NULL OR deleted!=1)))";

	private static final String _SQL_MEDIAFILES_WHERES_NOT_TAG =
			" NOT " + _SQL_MEDIAFILES_WHERES_TAG;

	private static final String _SQL_MEDIAFILES_WHERES_FILEORTAG =
			" (file LIKE ? ESCAPE ? OR id IN (SELECT mf_id FROM tbl_tags WHERE tag LIKE ? ESCAPE ? AND (deleted IS NULL OR deleted!=1)))";

	private static final String _SQL_MEDIAFILES_WHERENOTMISSING =
			" (missing<>1 OR missing is NULL)";

	private static final String _SQL_MEDIAFILES_WHEREENABLED =
			" (enabled<>0 OR enabled is NULL)";

	private static final String _SQL_MEDIAFILES_SEARCHORDERBY =
			" ORDER BY lastplay DESC, endcnt DESC, startcnt DESC, file COLLATE NOCASE ASC";

	private SearchParser () {
		throw new AssertionError();
	}

	/**
	 * Excludes missing and disabled. Ordered by lastplay, endcnt, startcnt,
	 * file.
	 */
	public static Search parseSearch (final MediaType mediaType, final String allTerms) {
		return parseSearch(mediaType, null, null, true, true, allTerms);
	}

	public static Search parseSearch (final MediaType mediaType, final String allTerms,
			final IDbColumn[] sort, final SortDirection[] direction) {
		return parseSearch(mediaType, sort, direction, true, true, allTerms);
	}

	public static Search parseSearch (final MediaType mediaType,
			final IDbColumn[] sort, final SortDirection[] direction,
			final boolean excludeMissing, final boolean excludeDisabled) {
		return parseSearch(mediaType, sort, direction, excludeMissing, excludeDisabled, null);
	}

	public static Search parseSearch (final MediaType mediaType,
			final IDbColumn[] sorts, final SortDirection[] directions,
			final boolean excludeMissing, final boolean excludeDisabled,
			final String allTerms) {
		if (sorts == null ^ directions == null) throw new IllegalArgumentException("Must specify both or neith of sort and direction.");
		if (sorts != null && directions != null && sorts.length != directions.length) throw new IllegalArgumentException("Sorts and directions must be same length.");

		final StringBuilder sql = new StringBuilder(_SQL_MEDIAFILES_SELECT);
		final List<String> terms = splitTerms(allTerms);
		//System.out.println(allTerms + " --> " + terms.size() + " terms: " + terms);
		appendWhere(sql, mediaType, excludeMissing, excludeDisabled, terms);
		if (sorts != null && directions != null && sorts.length > 0 && directions.length > 0) {
			sql.append(" ORDER BY ");
			for (int i = 0; i < sorts.length; i++) {
				if (i > 0) sql.append(",");
				sql.append(sorts[i].getName()).append(directions[i].getSql());
			}
		}
		else {
			sql.append(_SQL_MEDIAFILES_SEARCHORDERBY);
		}
		sql.append(";");
		return new Search(sql.toString(), mediaType, terms);
	}

	private static List<String> splitTerms (final String allTerms) {
		final List<String> terms = new ArrayList<String>();
		if (allTerms == null) return terms;

		final Matcher m = SEARCH_TERM_FINDER.matcher(allTerms);
		while (m.find()) {
			String g = m.group(1);
			if (g != null) {
				g = g.trim();
				if (g.length() > 0) terms.add(g);
				if (terms.size() >= MAX_SEARCH_TERMS) break;
			}
		}

		return terms;
	}

	private static void appendWhere (final StringBuilder sql, final MediaType mediaType, final boolean excludeMissing,
			final boolean excludeDisabled, final List<String> terms) {
		if (mediaType == MediaType.UNKNOWN && terms.size() < 1 && !excludeMissing && !excludeDisabled) return;

		sql.append(_SQL_WHERE);
		boolean needAnd = false;

		if (mediaType != MediaType.UNKNOWN) {
			if (needAnd) sql.append(_SQL_AND);
			needAnd = true;
			sql.append(_SQL_MEDIAFILES_WHERTYPE);
		}

		if (terms.size() > 0) {
			if (needAnd) sql.append(_SQL_AND);
			needAnd = true;

			sql.append(" ( ");
			int openBrackets = 0;
			for (int i = 0; i < terms.size(); i++) {
				final String term = terms.get(i);
				final String prevTerm = i > 0 ? terms.get(i - 1) : null;
				final String nextTerm = i < terms.size() - 1 ? terms.get(i + 1) : null;

				if ("OR".equals(term)) {
					if (prevTerm == null || nextTerm == null) continue; // Ignore leading and trailing.
					if ("OR".equals(prevTerm)) continue;
					if ("AND".equals(prevTerm)) continue;
					if ("(".equals(prevTerm)) continue;
					if (")".equals(nextTerm)) continue;
					sql.append(_SQL_OR);
					continue;
				}

				if ("AND".equals(term)) {
					if (prevTerm == null || nextTerm == null) continue; // Ignore leading and trailing.
					if ("OR".equals(prevTerm)) continue;
					if ("AND".equals(prevTerm)) continue;
					if ("(".equals(prevTerm)) continue;
					if (")".equals(nextTerm)) continue;
					sql.append(_SQL_AND);
					continue;
				}

				if (")".equals(term)) {
					if (openBrackets > 0) {
						sql.append(" ) ");
						openBrackets -= 1;
					}
					continue;
				}

				if (i > 0) {
					// Not the first term and not following OR or AND.
					if (!"OR".equals(prevTerm) && !"AND".equals(prevTerm) && !"(".equals(prevTerm)) {
						sql.append(_SQL_AND);
					}
				}

				if ("(".equals(term)) {
					sql.append(" ( ");
					openBrackets += 1;
				}
				else if (isFileMatchPartial(term)) {
					sql.append(_SQL_MEDIAFILES_WHERES_FILE);
				}
				else if (isFileNotMatchPartial(term)) {
					sql.append(_SQL_MEDIAFILES_WHERES_NOT_FILE);
				}
				else if (isTagMatchPartial(term) || isTagMatchExact(term)) {
					sql.append(_SQL_MEDIAFILES_WHERES_TAG);
				}
				else if (isTagNotMatchPartial(term) || isTagNotMatchExact(term)) {
					sql.append(_SQL_MEDIAFILES_WHERES_NOT_TAG);
				}
				else {
					sql.append(_SQL_MEDIAFILES_WHERES_FILEORTAG);
				}
			}

			// Tidy any unclosed brackets.
			for (int i = 0; i < openBrackets; i++) {
				sql.append(" ) ");
			}

			sql.append(" ) ");
		}

		if (excludeMissing) {
			if (needAnd) sql.append(_SQL_AND);
			needAnd = true;
			sql.append(_SQL_MEDIAFILES_WHERENOTMISSING);
		}

		if (excludeDisabled) {
			if (needAnd) sql.append(_SQL_AND);
			needAnd = true;
			sql.append(_SQL_MEDIAFILES_WHEREENABLED);
		}
	}

	protected static boolean isFileMatchPartial (final String term) {
		return term.startsWith("f~") || term.startsWith("F~");
	}

	protected static boolean isFileNotMatchPartial (final String term) {
		return term.startsWith("-f~") || term.startsWith("-F~");
	}

	protected static boolean isTagMatchPartial (final String term) {
		return term.startsWith("t~") || term.startsWith("T~");
	}

	protected static boolean isTagNotMatchPartial (final String term) {
		return term.startsWith("-t~") || term.startsWith("-T~");
	}

	protected static boolean isTagMatchExact (final String term) {
		return term.startsWith("t=") || term.startsWith("T=");
	}

	protected static boolean isTagNotMatchExact (final String term) {
		return term.startsWith("-t=") || term.startsWith("-T=");
	}

	protected static String removeMatchOperator (final String term) {
		int x = term.indexOf('=');
		if (x < 0) x = term.indexOf('~');
		if (x < 0) throw new IllegalArgumentException("term does not contain '=' or '~': " + term);
		return term.substring(x + 1);
	}

	protected static String anchoredOrWildcardEnds (final String term) {
		String ret = term;
		if (ret.startsWith("^")) {
			ret = ret.substring(1);
		}
		else {
			ret = "%" + ret;
		}
		if (ret.endsWith("$")) {
			ret = ret.substring(0, ret.length() - 1);
		}
		else {
			ret = ret + "%";
		}
		return ret;
	}

	protected static String unquote (final String term) {
		final Matcher m = PAIRED_QUOTES_FINDER.matcher(term);
		final StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String quoteType = null;
			String r = "";
			for (int i = 1; i <= m.groupCount(); i++) {
				final String g = m.group(i);
				if (g == null) continue;

				if (i == 2 || i == 4) {
					quoteType = g;
				}
				else {
					r += g;
				}
			}
			//System.out.println("q:" + quoteType + " r:" + r);
			r = r.replace("\\" + quoteType, quoteType);
			m.appendReplacement(sb, r.replace("\\", "\\\\"));
		}
		m.appendTail(sb);
		//System.out.println(term + " --> " + sb.toString());
		return sb.toString();
	}

	public static class Search {

		private final String sql;
		private final MediaType mediaType;
		private final List<String> terms;

		public Search (final String sql, final MediaType mediaType, final List<String> terms) {
			this.sql = sql;
			this.mediaType = mediaType;
			this.terms = terms;
		}

		private PreparedStatement prepare (final Connection con) throws SQLException {
			try {
				return con.prepareStatement(this.sql);
			}
			catch (final SQLException e) {
				throw new SQLException("Failed to compile query (sql='" + this.sql + "').", e);
			}
		}

		public List<IMixedMediaItem> execute (final Connection con, final MixedMediaItemFactory itemFactory) throws SQLException {
			return execute(con, itemFactory, -1);
		}

		public List<IMixedMediaItem> execute (final Connection con, final MixedMediaItemFactory itemFactory, final int maxResults) throws SQLException {
			final PreparedStatement ps = prepare(con);
			try {
				int parmIn = 1;
				if (this.mediaType != MediaType.UNKNOWN) ps.setInt(parmIn++, this.mediaType.getN());
				for (final String term : this.terms) {
					if ("OR".equals(term)) continue;
					if ("AND".equals(term)) continue;
					if ("(".equals(term)) continue;
					if (")".equals(term)) continue;
					if (isFileMatchPartial(term) || isFileNotMatchPartial(term)
							|| isTagMatchPartial(term) || isTagNotMatchPartial(term)) {
						ps.setString(parmIn++, anchoredOrWildcardEnds(SqliteHelper.escapeSearch(unquote(removeMatchOperator(term)))));
						ps.setString(parmIn++, SqliteHelper.SEARCH_ESC);
					}
					else if (isTagMatchExact(term) || isTagNotMatchExact(term)) {
						ps.setString(parmIn++, SqliteHelper.escapeSearch(unquote(removeMatchOperator(term))));
						ps.setString(parmIn++, SqliteHelper.SEARCH_ESC);
					}
					else {
						final String escapedTerm = SqliteHelper.escapeSearch(unquote(term));
						ps.setString(parmIn++, "%" + escapedTerm + "%");
						ps.setString(parmIn++, SqliteHelper.SEARCH_ESC);
						ps.setString(parmIn++, "%" + escapedTerm + "%");
						ps.setString(parmIn++, SqliteHelper.SEARCH_ESC);
					}
				}
				if (maxResults > 0) ps.setMaxRows(maxResults);
				final ResultSet rs = ps.executeQuery();
				try {
					return MixedMediaSqliteLayerInner.local_parseRecordSet(rs, itemFactory);
				}
				finally {
					rs.close();
				}
			}
			finally {
				ps.close();
			}
		}

		@Override
		public String toString () {
			return new StringBuilder("Search{")
					.append("sql=" + this.sql)
					.append(", mediaType=" + this.mediaType)
					.append(", terms=" + this.terms)
					.append("}")
					.toString();
		}

	}

}

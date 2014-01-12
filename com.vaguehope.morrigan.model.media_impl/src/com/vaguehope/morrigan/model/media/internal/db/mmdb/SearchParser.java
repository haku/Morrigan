package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;

class SearchParser {

	private static final int MAX_SEARCH_TERMS = 10;
	private static final Pattern SEARCH_TERM_SPLIT = Pattern.compile("(?:\\s|　)+");

	private static final String _SQL_MEDIAFILES_SELECT =
			"SELECT"
					+ " id,file,type,md5,added,modified,enabled,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
					+ " FROM tbl_mediafiles";

	private static final String _SQL_MEDIAFILES_WHERTYPE =
			" type=?";

	private static final String _SQL_MEDIAFILES_WHERES_FILE =
			" (file LIKE ? ESCAPE ?)";

	private static final String _SQL_MEDIAFILES_WHERES_TAG =
			" (id IN (SELECT mf_id FROM tbl_tags WHERE tag LIKE ? ESCAPE ?))";

	private static final String _SQL_MEDIAFILES_WHERES_FILEORTAG =
			" (file LIKE ? ESCAPE ? OR id IN (SELECT mf_id FROM tbl_tags WHERE tag LIKE ? ESCAPE ?))";

	private static final String _SQL_MEDIAFILESTAGS_WHERESEARCH_ANDEXTRA =
			" AND (missing<>1 OR missing is NULL) AND (enabled<>0 OR enabled is NULL)"
					+ " ORDER BY lastplay DESC, endcnt DESC, startcnt DESC, file COLLATE NOCASE ASC;";

	private static final String _SQL_WHERE = " WHERE";
	private static final String _SQL_AND = " AND";
	private static final String _SQL_OR = " OR";

	private SearchParser () {
		throw new AssertionError();
	}

	public static Search parseSearch (final MediaType mediaType, final String allTerms, final String esc) {
		final List<String> terms = new ArrayList<String>();
		for (final String subTerm : SEARCH_TERM_SPLIT.split(allTerms)) {
			if (subTerm != null && subTerm.length() > 0) terms.add(subTerm);
			if (terms.size() >= MAX_SEARCH_TERMS) break;
		}
		if (terms.size() < 1) throw new IllegalArgumentException("No search terms specified.");

		final StringBuilder sql = new StringBuilder().append(_SQL_MEDIAFILES_SELECT).append(_SQL_WHERE);
		if (mediaType != MediaType.UNKNOWN) sql.append(_SQL_MEDIAFILES_WHERTYPE).append(_SQL_AND);
		sql.append(" ( ");
		for (int i = 0; i < terms.size(); i++) {
			final String term = terms.get(i);
			if (i > 0) {
				if ("OR".equals(term)) {
					sql.append(_SQL_OR);
					continue;
				}
				else if (!"OR".equals(terms.get(i - 1))) {
					sql.append(_SQL_AND);
				}
			}
			if (term.startsWith("f~")) {
				sql.append(_SQL_MEDIAFILES_WHERES_FILE);
			}
			else if (term.startsWith("t~") || term.startsWith("t=")) {
				sql.append(_SQL_MEDIAFILES_WHERES_TAG);
			}
			else {
				sql.append(_SQL_MEDIAFILES_WHERES_FILEORTAG);
			}
		}
		sql.append(" ) ");
		sql.append(_SQL_MEDIAFILESTAGS_WHERESEARCH_ANDEXTRA);
		return new Search(sql.toString(), mediaType, terms, esc);
	}

	public static class Search {

		private final String sql;
		private final MediaType mediaType;
		private final List<String> terms;
		private final String esc;

		public Search (final String sql, final MediaType mediaType, final List<String> terms, final String esc) {
			this.sql = sql;
			this.mediaType = mediaType;
			this.terms = terms;
			this.esc = esc;
		}

		private PreparedStatement prepare (final Connection con) throws SQLException {
			try {
				return con.prepareStatement(this.sql);
			}
			catch (final SQLException e) {
				throw new SQLException("Failed to compile query (sql='" + this.sql + "').", e);
			}
		}

		public List<IMixedMediaItem> execute (final Connection con, final MixedMediaItemFactory itemFactory, final int maxResults) throws SQLException {
			final PreparedStatement ps = prepare(con);
			try {
				int parmIn = 1;
				if (this.mediaType != MediaType.UNKNOWN) ps.setInt(parmIn++, this.mediaType.getN());
				for (final String term : this.terms) {
					if ("OR".equals(term)) continue;
					if (term.startsWith("f~") || term.startsWith("t~")) {
						ps.setString(parmIn++, "%" + term.substring(2) + "%");
						ps.setString(parmIn++, this.esc);
					}
					else if (term.startsWith("t=")) {
						ps.setString(parmIn++, term.substring(2));
						ps.setString(parmIn++, this.esc);
					}
					else {
						ps.setString(parmIn++, "%" + term + "%");
						ps.setString(parmIn++, this.esc);
						ps.setString(parmIn++, "%" + term + "%");
						ps.setString(parmIn++, this.esc);
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
					.append(", esc=" + this.esc)
					.append("}")
					.toString();
		}

	}

}

package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;

class SearchParser {

	private static final int MAX_SEARCH_TERMS = 10;
	private static final Pattern SEARCH_TERM_SPLIT = Pattern.compile("(?:\\s|　)+");

	private static final String _SQL_MEDIAFILES_SELECT =
			"SELECT id,file,type,md5,added,modified,enabled,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
					+ " FROM tbl_mediafiles";

	private static final String _SQL_WHERE = " WHERE";
	private static final String _SQL_AND = " AND";
	private static final String _SQL_OR = " OR";

	private static final String _SQL_MEDIAFILES_WHERTYPE =
			" type=?";

	private static final String _SQL_MEDIAFILES_WHERES_FILE =
			" (file LIKE ? ESCAPE ?)";

	private static final String _SQL_MEDIAFILES_WHERES_TAG =
			" (id IN (SELECT mf_id FROM tbl_tags WHERE tag LIKE ? ESCAPE ?))";

	private static final String _SQL_MEDIAFILES_WHERES_FILEORTAG =
			" (file LIKE ? ESCAPE ? OR id IN (SELECT mf_id FROM tbl_tags WHERE tag LIKE ? ESCAPE ?))";

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
	public static Search parseSearch (final MediaType mediaType, final String allTerms, final String esc) {
		return parseSearch(mediaType, null, null, true, true, allTerms, esc);
	}

	public static Search parseSearch (final MediaType mediaType,
			final IDbColumn sort, final SortDirection direction,
			final boolean excludeMissing, final boolean excludeDisabled) {
		return parseSearch(mediaType, sort, direction, excludeMissing, excludeDisabled, null, null);
	}

	public static Search parseSearch (final MediaType mediaType,
			final IDbColumn sort, final SortDirection direction,
			final boolean excludeMissing, final boolean excludeDisabled,
			final String allTerms, final String esc) {
		if (sort == null ^ direction == null) throw new IllegalArgumentException("Must specify both or neith of sort and direction.");

		final StringBuilder sql = new StringBuilder(_SQL_MEDIAFILES_SELECT);
		final List<String> terms = splitTerms(allTerms);
		appendWhere(sql, mediaType, excludeMissing, excludeDisabled, terms);
		if (sort != null && direction != null) {
			sql.append(" ORDER BY ").append(sort.getName()).append(direction.getSql());
		}
		else {
			sql.append(_SQL_MEDIAFILES_SEARCHORDERBY);
		}
		sql.append(";");
		return new Search(sql.toString(), mediaType, terms, esc);
	}

	private static List<String> splitTerms (final String allTerms) {
		final List<String> terms = new ArrayList<String>();
		if (allTerms == null) return terms;
		for (final String subTerm : SEARCH_TERM_SPLIT.split(allTerms)) {
			if (subTerm != null && subTerm.length() > 0) terms.add(subTerm);
			if (terms.size() >= MAX_SEARCH_TERMS) break;
		}
		return terms;
	}

	private static void appendWhere (final StringBuilder sql, final MediaType mediaType, final boolean excludeMissing, final boolean excludeDisabled, final List<String> terms) {
		if (mediaType == MediaType.UNKNOWN && terms.size() < 1 && !excludeMissing && !excludeDisabled) return;

		sql.append(_SQL_WHERE);
		if (mediaType != MediaType.UNKNOWN) sql.append(_SQL_MEDIAFILES_WHERTYPE);

		if (terms.size() > 0) {
			sql.append(_SQL_AND);
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
		}

		if (excludeMissing) sql.append(_SQL_AND).append(_SQL_MEDIAFILES_WHERENOTMISSING);
		if (excludeDisabled) sql.append(_SQL_AND).append(_SQL_MEDIAFILES_WHEREENABLED);
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

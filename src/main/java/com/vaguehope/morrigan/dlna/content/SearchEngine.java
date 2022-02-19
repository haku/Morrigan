package com.vaguehope.morrigan.dlna.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.cdsc.CDSCBaseListener;
import com.vaguehope.cdsc.CDSCLexer;
import com.vaguehope.cdsc.CDSCParser;
import com.vaguehope.cdsc.CDSCParser.RelExpContext;
import com.vaguehope.morrigan.dlna.util.Cache;
import com.vaguehope.morrigan.dlna.util.Objects;
import com.vaguehope.morrigan.dlna.util.StringHelper;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class SearchEngine {

	private static final int MAX_RESULTS = 1000;
	protected static final Logger LOG = LoggerFactory.getLogger(SearchEngine.class);

	private final ContentAdaptor contentAdaptor;
	private final MediaFactory mediaFactory;

	public SearchEngine (final ContentAdaptor contentAdaptor, final MediaFactory mediaFactory) {
		this.contentAdaptor = contentAdaptor;
		this.mediaFactory = mediaFactory;
	}

	private final Cache<String, List<Item>> queryCache = new Cache<String, List<Item>>(50);

	public List<Item> search (final ContentNode contentNode, final String searchCriteria) throws ContentDirectoryException, DbException, MorriganException {
		if (searchCriteria == null) throw new ContentDirectoryException(ContentDirectoryErrorCodes.UNSUPPORTED_SEARCH_CRITERIA, "Do not know how to parse: " + searchCriteria);

		final String term = criteriaToMnTerm(searchCriteria);
		if (term == null) throw new ContentDirectoryException(ContentDirectoryErrorCodes.UNSUPPORTED_SEARCH_CRITERIA, "Do not know how to parse: " + searchCriteria);

		final List<Item> ret = new ArrayList<Item>();
		for (final MediaListReference mlr : this.mediaFactory.getAllLocalMixedMediaDbs()) {
			final IMixedMediaDb db = this.mediaFactory.getLocalMixedMediaDb(mlr.getIdentifier());
			if (db.getCount() > 0) { // Only search loaded DBs.
				final String cacheKey = String.format("%s|%s|%s", term, contentNode.getContainer().getId(), mlr.getIdentifier());
				List<Item> results = this.queryCache.getFresh(cacheKey, 1, TimeUnit.MINUTES);
				if (results == null) {
					results = this.contentAdaptor.queryToItems(mlr, db, term, contentNode.getContainer(), MAX_RESULTS);
					this.queryCache.put(cacheKey, results);
				}
				ret.addAll(results);
			}
		}

		LOG.info("se: {} --> {} ({} results).", searchCriteria, term, ret.size());
		return ret;
	}

	protected static String criteriaToMnTerm (final String searchCriteria) {
		final CriteriaListener listener = new CriteriaListener();
		new ParseTreeWalker().walk(listener, new CDSCParser(
				new CommonTokenStream(new CDSCLexer(new ANTLRInputStream(searchCriteria)))
				).searchCrit());
		return listener.getSearch();
	}

	private static class CriteriaListener extends CDSCBaseListener {

		private static final Set<String> TITLE_FIELDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
				"dc:title")));

		private static final Set<String> ARTIST_FIELDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
				"dc:creator", "upnp:artist")));

		private final Set<Predicate> allPredicates = new LinkedHashSet<Predicate>();

		public CriteriaListener () {}

		public String getSearch () {
			if (this.allPredicates == null || this.allPredicates.size() < 1) return null;
			return new And(this.allPredicates).toString();
		}

		@Override
		public void enterRelExp (@NotNull final RelExpContext ctx) {
			final String propertyName = ctx.Property().getText();
			final String op = ctx.binOp().getText();
			final String value = StringHelper.unquoteQuotes(ctx.QuotedVal().getText());

			if ("upnp:class".equals(propertyName)) {
				if ("=".equals(op) || "derivedfrom".equalsIgnoreCase(op)) {
					relExpDerivedfrom(propertyName, value);
				}
				else {
					LOG.debug("Unsupported op for property {}: {}", propertyName, op);
				}
			}
			else if (TITLE_FIELDS.contains(propertyName)) {
				if ("=".equals(op) || "contains".equalsIgnoreCase(op)) {
					this.allPredicates.add(new AnyContains(value));
				}
				else {
					LOG.debug("Unsupported op for property {}: {}", propertyName, op);
				}
			}
			else if (ARTIST_FIELDS.contains(propertyName)) {
				if ("=".equals(op) || "contains".equalsIgnoreCase(op)) {
					this.allPredicates.add(new TagContains(value));
				}
				else {
					LOG.debug("Unsupported op for property {}: {}", propertyName, op);
				}
			}
			else {
				LOG.debug("Unsupported property: {}", propertyName);
			}
		}

		private void relExpDerivedfrom (final String propertyName, final String value) {
			/*
			 * TODO
			 * value may be:
			 * - object.item.videoItem
			 * - object.item.audioItem
			 * - object.container.album.musicAlbum
			 * - object.container.person.musicArtist
			 */
			LOG.debug("Unsupported value for property {}: {}", propertyName, value);
		}

	}

	protected interface Predicate { /* Just a marker. */}

	private static class And implements Predicate {

		private final Collection<Predicate> predicates;

		public And (final Collection<Predicate> predicates) {
			this.predicates = predicates;
		}

		@Override
		public String toString () {
			return StringHelper.join(this.predicates, " ");
		}

		@Override
		public int hashCode () {
			return this.predicates.hashCode();
		}

		@Override
		public boolean equals (final Object obj) {
			if (obj == null) return false;
			if (obj == this) return true;
			if (!(obj instanceof And)) return false;
			final And that = (And) obj;
			return Objects.equals(this.predicates, that.predicates);
		}

	}

	private static class AnyContains implements Predicate {

		private final String lcaseSubString;

		public AnyContains (final String subString) {
			this.lcaseSubString = subString.toLowerCase(Locale.ENGLISH);
		}

		@Override
		public String toString () {
			return this.lcaseSubString;
		}

		@Override
		public int hashCode () {
			return this.lcaseSubString.hashCode();
		}

		@Override
		public boolean equals (final Object obj) {
			if (obj == null) return false;
			if (obj == this) return true;
			if (!(obj instanceof AnyContains)) return false;
			final AnyContains that = (AnyContains) obj;
			return Objects.equals(this.lcaseSubString, that.lcaseSubString);
		}

	}

	private static class TagContains implements Predicate {

		private final String lcaseSubString;

		public TagContains (final String subString) {
			this.lcaseSubString = subString.toLowerCase(Locale.ENGLISH);
		}

		@Override
		public String toString () {
			return String.format("t~\"%s\"", this.lcaseSubString);
		}

		@Override
		public int hashCode () {
			return this.lcaseSubString.hashCode();
		}

		@Override
		public boolean equals (final Object obj) {
			if (obj == null) return false;
			if (obj == this) return true;
			if (!(obj instanceof TagContains)) return false;
			final TagContains that = (TagContains) obj;
			return Objects.equals(this.lcaseSubString, that.lcaseSubString);
		}

	}

}

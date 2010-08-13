package net.sparktank.morrigan.model.media.interfaces;

import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.DbColumn;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.sqlitewrapper.DbException;

public interface IMixedMediaDb<S extends IMixedMediaStorageLayer, T extends IMixedMediaItem> extends IMixedMediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum SortDirection { ASC, DESC };
	
	public interface SortChangeListener {
		public void sortChanged (DbColumn sort, SortDirection direction);
	}
	
	public S getStorageLayer();
	
	public DbColumn getSort ();
	public SortDirection getSortDirection ();
	public void setSort (DbColumn sort, SortDirection direction) throws MorriganException;
	public void registerSortChangeListener (SortChangeListener scl);
	public void unregisterSortChangeListener (SortChangeListener scl);
	
	public List<T> simpleSearch (String term, String esc, int maxResults) throws DbException;
	
	public String getDbPath ();
	public List<String> getSources () throws DbException;
	public void addSource (String source) throws DbException;
	public void removeSource (String source) throws DbException;
	
	public boolean hasTags (IDbItem item) throws MorriganException;
	public boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException;
	public List<MediaTag> getTags (IDbItem item) throws MorriganException;
	public void addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException;
	public void addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws MorriganException;
	public void moveTags (IDbItem from_item, IDbItem to_item) throws MorriganException;
	public void removeTag (MediaTag mt) throws MorriganException;
	public void clearTags (IDbItem item) throws MorriganException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

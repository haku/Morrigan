package net.sparktank.morrigan.model.media.interfaces;

import java.io.File;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer.SortDirection;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.sqlitewrapper.DbException;

public interface IMediaItemDb<H extends IMediaItemDb<H,S,T>, S extends IMediaItemStorageLayer<T>, T extends IMediaItem> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public interface SortChangeListener {
		public void sortChanged (IDbColumn sort, SortDirection direction);
	}
	
	public H getTransactionalClone () throws DbException;
	public void commitOrRollback () throws DbException;
	public void rollback () throws DbException;
	
	public String getDbPath ();
	public S getDbLayer();
	
	public List<String> getSources () throws MorriganException;
	public void addSource (String source) throws MorriganException;
	public void removeSource (String source) throws MorriganException;
	
	public List<T> simpleSearch (String term, String esc, int maxResults) throws DbException;
	public List<T> getAllDbEntries () throws DbException;
	
	public T addFile (File file) throws MorriganException, DbException;
	public boolean hasFile (File file) throws MorriganException, DbException;
	public List<T> addFiles (List<File> files) throws MorriganException, DbException;
	
	public IDbColumn getSort ();
	public SortDirection getSortDirection ();
	public void setSort (IDbColumn sort, SortDirection direction) throws MorriganException;
	public void registerSortChangeListener (SortChangeListener scl);
	public void unregisterSortChangeListener (SortChangeListener scl);
	
	public List<MediaTagClassification> getTagClassifications () throws MorriganException;
	public void addTagClassification (String classificationName) throws MorriganException;
	public MediaTagClassification getTagClassification (String classificationName) throws MorriganException;
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

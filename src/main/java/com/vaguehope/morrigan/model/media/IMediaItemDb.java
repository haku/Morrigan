package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.sqlitewrapper.DbException;


public interface IMediaItemDb extends IMediaItemList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	interface SortChangeListener {
		void sortChanged (IDbColumn sort, SortDirection direction);
	}

	void commitOrRollback () throws DbException;
	void rollback () throws DbException;

	String getDbPath ();
	IMediaItemStorageLayer getDbLayer();

	List<String> getSources () throws MorriganException;
	void addSource (String source) throws MorriganException;
	void removeSource (String source) throws MorriganException;

	void addRemote(String name, URI uri) throws DbException;
	void rmRemote(String name) throws DbException;
	URI getRemote(String name) throws DbException;
	Map<String, URI> getRemotes() throws DbException;

	List<IMediaItem> simpleSearch (MediaType mediaType, String term, int maxResults) throws DbException;
	List<IMediaItem> simpleSearch (MediaType mediaType, String term, int maxResults, IDbColumn[] sortColumns, SortDirection[] sortDirections, boolean includeDisabled) throws DbException;
	List<IMediaItem> getAllDbEntries () throws DbException;
	IMediaItem getByFile (File file) throws DbException;
	IMediaItem getByFile (String filepath) throws DbException;
	IMediaItem getByMd5 (BigInteger md5) throws DbException;

	/**
	 * Note: returned item will be missing DB rowId.
	 */
	IMediaItem addFile (MediaType mediaType, File file) throws MorriganException, DbException;
	FileExistance hasFile (String filepath) throws MorriganException, DbException;
	FileExistance hasFile (File file) throws MorriganException, DbException;
	List<IMediaItem> addFiles (MediaType mediaType, List<File> files) throws MorriganException, DbException;

	void setHideMissing(final boolean v) throws MorriganException;

	IDbColumn getSort ();
	SortDirection getSortDirection ();
	void setSort (IDbColumn sort, SortDirection direction) throws MorriganException;
	void registerSortChangeListener (SortChangeListener scl);
	void unregisterSortChangeListener (SortChangeListener scl);

	boolean isMarkedAsUnreadable (IMediaItem mi) throws MorriganException;
	void markAsUnreadabled (IMediaItem mi) throws MorriganException;

	void beginBulkUpdate ();
	void completeBulkUpdate (boolean thereWereErrors) throws MorriganException, DbException;
	IMediaItem updateItem (IMediaItem item) throws MorriganException, DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

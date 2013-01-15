package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.sqlitewrapper.DbException;


public interface IMediaItemDb<S extends IMediaItemStorageLayer<T>, T extends IMediaItem> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	interface SortChangeListener {
		void sortChanged (IDbColumn sort, SortDirection direction);
	}

	void commitOrRollback () throws DbException;
	void rollback () throws DbException;

	String getDbPath ();
	S getDbLayer();

	List<String> getSources () throws MorriganException;
	void addSource (String source) throws MorriganException;
	void removeSource (String source) throws MorriganException;

	List<T> simpleSearch (String term, int maxResults) throws DbException;
	List<T> getAllDbEntries () throws DbException;
	T getByFile (File file) throws DbException;
	T getByFile (String filepath) throws DbException;

	T addFile (File file) throws MorriganException, DbException;
	boolean hasFile (String filepath) throws MorriganException, DbException;
	boolean hasFile (File file) throws MorriganException, DbException;
	List<T> addFiles (List<File> files) throws MorriganException, DbException;

	IDbColumn getSort ();
	SortDirection getSortDirection ();
	void setSort (IDbColumn sort, SortDirection direction) throws MorriganException;
	void registerSortChangeListener (SortChangeListener scl);
	void unregisterSortChangeListener (SortChangeListener scl);

	boolean isMarkedAsUnreadable (T mi) throws MorriganException;
	void markAsUnreadabled (T mi) throws MorriganException;

	void beginBulkUpdate ();
	void completeBulkUpdate (boolean thereWereErrors) throws MorriganException, DbException;
	T updateItem (T item) throws MorriganException, DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

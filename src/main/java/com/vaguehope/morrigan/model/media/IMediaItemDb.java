package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
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

	/**
	 * Returns a copy of the main list updated with all items from the DB.
	 */
	List<MediaItem> getAllDbEntries () throws DbException;

	MediaItem getByFile (File file) throws DbException;

	/**
	 * Note: returned item will be missing DB rowId.
	 */
	MediaItem addFile (MediaType mediaType, File file) throws MorriganException, DbException;
	FileExistance hasFile (File file) throws MorriganException, DbException;
	List<MediaItem> addFiles (MediaType mediaType, List<File> files) throws MorriganException, DbException;

	void setHideMissing(final boolean v) throws MorriganException;

	boolean isMarkedAsUnreadable (MediaItem mi) throws MorriganException;
	void markAsUnreadabled (MediaItem mi) throws MorriganException;

	void beginBulkUpdate ();
	void completeBulkUpdate (boolean thereWereErrors) throws MorriganException, DbException;
	MediaItem updateItem (MediaItem item) throws MorriganException, DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

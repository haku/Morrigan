package morrigan.model.media;

import java.io.File;
import java.util.List;

import morrigan.model.db.IDbColumn;
import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.MediaItem.MediaType;
import morrigan.model.media.SortColumn.SortDirection;
import morrigan.sqlitewrapper.DbException;


public interface MediaDb extends MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	interface SortChangeListener {
		void sortChanged (IDbColumn sort, SortDirection direction);
	}

	void commitOrRollback () throws DbException;
	void rollback () throws DbException;

	String getDbPath ();
	MediaStorageLayer getDbLayer();

	List<String> getSources () throws MorriganException;
	void addSource (String source) throws MorriganException;
	void removeSource (String source) throws MorriganException;

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

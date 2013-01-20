package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.sqlitewrapper.DbException;
import com.vaguehope.sqlitewrapper.IGenericDbLayer;

public interface IMediaItemStorageLayer<T extends IMediaItem> extends IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public enum SortDirection {
		ASC(0), DESC(1);

		private final int n;

		SortDirection (int n) {
			this.n = n;
		}

		public int getN () {
			return this.n;
		}

		public static SortDirection parseN (int n) {
			switch (n) {
				case 0: return ASC;
				case 1: return DESC;
				default: throw new IllegalArgumentException();
			}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void addChangeListener (IMediaItemStorageLayerChangeListener<T> listener);
	void removeChangeListener (IMediaItemStorageLayerChangeListener<T> listener);
	IMediaItemStorageLayerChangeListener<T> getChangeEventCaller ();

	void setProp (String key, String value) throws DbException;
	String getProp (String key) throws DbException;

	List<String> getSources () throws DbException;
	void addSource (String source) throws DbException;
	void removeSource (String source) throws DbException;

	List<MediaTagClassification> getTagClassifications () throws DbException;
	void addTagClassification (String classificationName) throws DbException;
	MediaTagClassification getTagClassification (String classificationName) throws DbException;
	boolean hasTags (IDbItem item) throws DbException;
	boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException;
	List<MediaTag> getTags (IDbItem item) throws DbException;
	boolean addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException;
	boolean addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws DbException;
	void moveTags (IDbItem fromItem, IDbItem toItem) throws DbException;
	void removeTag (MediaTag tag) throws DbException;
	void clearTags (IDbItem item) throws DbException;

	/**
	 * Returns existing album if it already exists.
	 * Name is case-insensitive.
	 */
	MediaAlbum createAlbum (String name) throws DbException;
	/**
	 * Get album, or null if not found.
	 */
	MediaAlbum getAlbum (String name) throws DbException;
	void removeAlbum (MediaAlbum album) throws DbException;
	Collection<MediaAlbum> getAlbums () throws DbException;
	Collection<T> getAlbumItems (MediaAlbum album) throws DbException;
	/**
	 * Will have no effect if already in album.
	 */
	void addToAlbum (MediaAlbum album, IDbItem item) throws DbException;
	void removeFromAlbum (MediaAlbum album, IDbItem item) throws DbException;
	void removeFromAllAlbums (IDbItem item) throws DbException;

	List<IDbColumn> getMediaTblColumns ();

	/**
	 * Will always return the same value; i.e. it returns a constant.
	 */
	IDbColumn getDefaultSortColumn ();

	List<T> getAllMedia (IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	List<T> getMedia (IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	List<T> getMedia (IDbColumn sort, SortDirection direction, boolean hideMissing, String search, String searchEsc) throws DbException;

	boolean hasFile (File file) throws DbException;
	boolean hasFile (String filePath) throws DbException;
	T getByFile (File file) throws DbException;
	T getByFile (String filePath) throws DbException;
	List<T> simpleSearch(String term, String esc, int maxResults) throws DbException;

	boolean[] addFiles (List<File> files) throws DbException;

	/**
	 *
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	boolean addFile(File file) throws DbException;

	/**
	 *
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	boolean addFile(String filepath, long lastModified) throws DbException;

	int removeFile(String sfile) throws DbException;
	int removeFile (IDbItem dbItem) throws DbException;

	void setDateAdded(String sfile, Date date) throws DbException;
	void setHashcode(String sfile, BigInteger hashcode) throws DbException;
	void setDateLastModified(String sfile, Date date) throws DbException;
	void setEnabled(String sfile, boolean value) throws DbException;
	void setMissing(String sfile, boolean value) throws DbException;
	void setRemoteLocation(String sfile, String remoteLocation) throws DbException;

	T getNewT(String filePath);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
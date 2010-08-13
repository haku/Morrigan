package net.sparktank.morrigan.model.media.interfaces;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbItem;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.sqlitewrapper.DbException;
import net.sparktank.sqlitewrapper.IGenericDbLayer;

public interface IMediaItemStorageLayer<T extends IMediaItem> extends IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setProp (String key, String value) throws DbException;
	public String getProp (String key) throws DbException;
	
	public List<String> getSources () throws DbException;
	public void addSource (String source) throws DbException;
	public void removeSource (String source) throws DbException;
	
	public boolean hasTags (IDbItem item) throws DbException;
	public boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException;
	public List<MediaTag> getTags (IDbItem item) throws DbException;
	public List<MediaTagClassification> getTagClassifications () throws DbException;
	public boolean addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException;
	public boolean addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws DbException;
	public void moveTags (IDbItem from_item, IDbItem to_item) throws DbException;
	public void removeTag (MediaTag tag) throws DbException;
	public void clearTags (IDbItem item) throws DbException;
	public void addTagClassification (String classificationName) throws DbException;
	
	public List<T> getAllMedia(IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<T> updateListOfAllMedia(List<T> list, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<T> simpleSearch(String term, String esc, int maxResults) throws DbException;
	
	/**
	 * 
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile(File file) throws DbException;
	
	/**
	 * 
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile(String filepath, long lastModified) throws DbException;
	
	public int removeFile(String sfile) throws DbException;
	public int removeFile (IDbItem dbItem) throws DbException;
	
	public void setDateAdded(String sfile, Date date) throws DbException;
	public void setHashcode(String sfile, long hashcode) throws DbException;
	public void setDateLastModified(String sfile, Date date) throws DbException;
	public void setEnabled(String sfile, boolean value) throws DbException;
	public void setMissing(String sfile, boolean value) throws DbException;
	public void setRemoteLocation(String sfile, String remoteLocation) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
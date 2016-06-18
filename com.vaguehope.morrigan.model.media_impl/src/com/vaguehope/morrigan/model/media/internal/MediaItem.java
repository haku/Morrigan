package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;

import com.vaguehope.morrigan.model.helper.EqualHelper;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.Objs;


/**
 * Generic media item, be it music, video, image, etc...
 */
public abstract class MediaItem implements IMediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.

	private static final BigInteger HASHCODE_DEFAULT = null;
	private static final boolean MISSING_DEFAULT = false;
	private static final boolean ENABLED_DEFAULT = true;
	private static final int DBROWID_DEFAULT = -1;

	public MediaItem (final String filePath) {
		setFilepath(filePath);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.

	private String filepath = null;
	private Date dateAdded = null;
	private BigInteger hashcode = HASHCODE_DEFAULT;
	private Date dateLastModified = null;
	private boolean enabled = ENABLED_DEFAULT;
	private Date enabledLastModified = null;
	private boolean missing = MISSING_DEFAULT;

	@Override
	public long getFileSize () {
		return new File(this.filepath).length();
	}

	@Override
	public String getFilepath () {
		return this.filepath;
	}
	@Override
	public boolean setFilepath (final String filepath) {
		if (this.filepath == null && filepath != null) {
			return setFilepathIfNotSet(filepath);
		}
		else if (EqualHelper.areEqual(this.filepath, filepath)) {
			return false;
		}
		else {
			throw new IllegalStateException("filepath can not be modified once set.  Current='"+this.filepath+"' proposed='"+filepath+"'.");
		}
	}

	/**
	 * This will silently fail if filepath is already set.
	 */
	private boolean setFilepathIfNotSet (final String filepath) {
		if (this.filepath == null && !EqualHelper.areEqual(this.filepath, filepath)) {
			this.filepath = filepath;
			updateTitle();
			return true;
		}
		return false;
	}

	@Override
	public String getMimeType () {
		return null;
	}

	@Override
	public Date getDateAdded() {
		return this.dateAdded;
	}
	@Override
	public boolean setDateAdded(final Date dateAdded) {
		if (!EqualHelper.areEqual(this.dateAdded, dateAdded)) {
			this.dateAdded = dateAdded;
			return true;
		}
		return false;
	}

	@Override
	public BigInteger getHashcode() {
		return this.hashcode;
	}
	@Override
	public boolean setHashcode(final BigInteger newHashcode) {
		if (!EqualHelper.areEqual(this.hashcode, newHashcode)) {
			this.hashcode = newHashcode;
			return true;
		}
		return false;
	}

	@Override
	public Date getDateLastModified() {
		return this.dateLastModified;
	}
	@Override
	public boolean setDateLastModified(final Date lastModified) {
		if (!EqualHelper.areEqual(this.dateLastModified, lastModified)) {
			this.dateLastModified = lastModified;
			return true;
		}
		return false;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}
	@Override
	public Date enabledLastModified () {
		return this.enabledLastModified;
	}
	@Override
	public boolean setEnabled(final boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			return true;
		}
		return false;
	}
	@Override
	public boolean setEnabled (final boolean enabled, final Date lastModified) {
		if (this.enabled != enabled || !Objs.equals(this.enabledLastModified, lastModified)) {
			this.enabled = enabled;
			this.enabledLastModified = lastModified;
			return true;
		}
		return false;
	}

	@Override
	public boolean isMissing() {
		return this.missing;
	}
	@Override
	public boolean setMissing(final boolean missing) {
		if (this.missing != missing) {
			this.missing = missing;
			return true;
		}
		return false;
	}

//	-  -  -  -  -  -  -  -

	private long dbRowId = DBROWID_DEFAULT;
	private String remoteLocation;

	@Override
	public long getDbRowId() {
		return this.dbRowId;
	}
	@Override
	public boolean setDbRowId(final long dbRowId) {
		/* Sqlite ROWID starts at 1, so if something tries to set it
		 * less than this, don't let them clear it.
		 * This is most likely when fetching a remote list over HTTP.
		 */
		if (dbRowId > 0 && this.dbRowId != dbRowId) {
			this.dbRowId = dbRowId;
			return true;
		}
		return false;
	}

	@Override
	public String getRemoteLocation() {
		return this.remoteLocation;
	}
	@Override
	public boolean setRemoteLocation(final String remoteLocation) {
		if (!EqualHelper.areEqual(this.remoteLocation, remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}

	@Override
	public String getRemoteId () {
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Secondary attributes.

	private String title = null;

	private void updateTitle () {
		int x = this.filepath.lastIndexOf(File.separator);
		if (x>0) {
			this.title = this.filepath.substring(x+1);
		}
		else {
			this.title = this.filepath;
		}
	}

	@Override
	public String getTitle () {
		return this.title;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.

	@Override
	public void reset () {
		this.setDateAdded(null);
		this.setHashcode(HASHCODE_DEFAULT);
		this.setDateLastModified(null);
		this.setEnabled(ENABLED_DEFAULT, null);
		this.setMissing(MISSING_DEFAULT);
		this.setDbRowId(DBROWID_DEFAULT);
		this.setRemoteLocation(null);
	}

	@Override
	public boolean setFromMediaItem (final IMediaItem mi) {
		boolean b =
    		  this.setFilepathIfNotSet(mi.getFilepath()) // Do not set it if it is already set.
    		| this.setDateAdded(mi.getDateAdded())
    		| this.setHashcode(mi.getHashcode())
    		| this.setDateLastModified(mi.getDateLastModified())
    		| this.setEnabled(mi.isEnabled(), mi.enabledLastModified())
    		| this.setMissing(mi.isMissing())
    		| this.setDbRowId(mi.getDbRowId())
    		| this.setRemoteLocation(mi.getRemoteLocation());
    	return b;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public boolean equals(final Object aThat) {
		if (aThat == null) return false;
		if (this == aThat) return true;
		if (!(aThat instanceof MediaItem)) return false;
		final MediaItem that = (MediaItem) aThat;

		return Objs.equals(getFilepath(), that.getFilepath());
	}

	@Override
	public int hashCode() {
		return getFilepath().hashCode();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String toString () {
		return getTitle();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

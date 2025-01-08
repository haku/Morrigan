package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.StringHelper;

public class DefaultMediaItem implements IMediaItem {

	private static final BigInteger MD5_DEFAULT = null;
	private static final BigInteger SHA1_DEFAULT = null;
	private static final boolean MISSING_DEFAULT = false;
	private static final boolean ENABLED_DEFAULT = true;
	private static final int DBROWID_DEFAULT = -1;

	private final IMediaItemList list;

	public DefaultMediaItem(final String filePath, final IMediaItemList list) {
		this.list = list;
		setFilepath(filePath);
	}

	//	Attributes.

	private String filepath = null;
	private Date dateAdded = null;
	private BigInteger md5 = MD5_DEFAULT;
	private BigInteger sha1 = SHA1_DEFAULT;
	private Date dateLastModified = null;
	private boolean enabled = ENABLED_DEFAULT;
	private Date enabledLastModified = null;
	private boolean missing = MISSING_DEFAULT;

	private MediaType type;
	private int duration;
	private long startCount;
	private long endCount;
	private Date dateLastPlayed;

	@Override
	public String getFilepath() {
		return this.filepath;
	}

	@Override
	public boolean setFilepath(final String filepath) {
		if (this.filepath == null && filepath != null) {
			return setFilepathIfNotSet(filepath);
		}
		else if (Objects.equals(this.filepath, filepath)) {
			return false;
		}
		else {
			throw new IllegalStateException("filepath can not be modified once set.  Current='" + this.filepath + "' proposed='" + filepath + "'.");
		}
	}

	/**
	 * This will silently fail if filepath is already set.
	 */
	private boolean setFilepathIfNotSet(final String filepath) {
		if (this.filepath == null && !Objects.equals(this.filepath, filepath)) {
			this.filepath = filepath;
			updateFilepathBasedThings();
			return true;
		}
		return false;
	}

	@Override
	public Date getDateAdded() {
		return this.dateAdded;
	}

	@Override
	public boolean setDateAdded(final Date dateAdded) {
		if (!Objects.equals(this.dateAdded, dateAdded)) {
			this.dateAdded = dateAdded;
			return true;
		}
		return false;
	}

	@Override
	public BigInteger getMd5() {
		return this.md5;
	}

	@Override
	public boolean setMd5(final BigInteger newMd5) {
		if (!Objects.equals(this.md5, newMd5)) {
			this.md5 = newMd5;
			return true;
		}
		return false;
	}

	@Override
	public BigInteger getSha1() {
		return this.sha1;
	}

	@Override
	public boolean setSha1(final BigInteger newSha1) {
		if (!Objects.equals(this.sha1, newSha1)) {
			this.sha1 = newSha1;
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
		if (!Objects.equals(this.dateLastModified, lastModified)) {
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
	public Date enabledLastModified() {
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
	public boolean setEnabled(final boolean enabled, final Date lastModified) {
		if (this.enabled != enabled || !Objects.equals(this.enabledLastModified, lastModified)) {
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
		/*
		 * Sqlite ROWID starts at 1, so if something tries to set it
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
	public boolean hasRemoteLocation() {
		return StringHelper.notBlank(this.remoteLocation);
	}

	@Override
	public String getRemoteLocation() {
		return this.remoteLocation;
	}

	@Override
	public boolean setRemoteLocation(final String remoteLocation) {
		if (!Objects.equals(this.remoteLocation, remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}

	@Override
	public String getRemoteId() {
		return null;
	}

	@Override
	public List<MediaTag> getTags() throws MorriganException {
		return this.list.getTags(this);
	}

	//	Secondary attributes.

	private File file = null;
	private long fileSize = -1;
	private String title = null;
	private String mimeType = null;

	private void updateFilepathBasedThings() {
		this.file = new File(this.filepath);
		this.fileSize = -1;

		final int x = this.filepath.lastIndexOf(File.separator);
		if (x > 0) {
			this.title = this.filepath.substring(x + 1);
		}
		else {
			this.title = this.filepath;
		}
	}

	@Override
	public File getFile() {
		return this.file;
	}

	@Override
	public long getFileSize() {
		if (this.fileSize < 0) this.fileSize = getFile().length();
		return this.fileSize;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	@Override
	public boolean hasMimeType() {
		return this.mimeType != null;
	}

	@Override
	public String getMimeType() {
		if (this.mimeType == null) {
			final MimeType id = MimeType.identify(this.filepath);
			if (id != null) return id.getMimeType();
		}
		return this.mimeType;
	}

	@Override
	public void setMimeType(final String newType) {
		this.mimeType = newType;
	}

	@Override
	public MediaType getMediaType () {
		return this.type;
	}

	@Override
	public boolean setMediaType (final MediaType newType) {
		if (this.type != newType) {
			this.type = newType;
			return true;
		}
		return false;
	}

	@Override
	public boolean isPlayable () {
		return (getMediaType() == MediaType.TRACK);
	}

	@Override
	public int getDuration () {
		return this.duration;
	}

	@Override
	public boolean setDuration (final int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			return true;
		}
		return false;
	}

	@Override
	public long getStartCount () {
		return this.startCount;
	}

	@Override
	public boolean setStartCount (final long startCount) {
		if (this.startCount != startCount) {
			this.startCount = startCount;
			return true;
		}
		return false;
	}

	@Override
	public long getEndCount () {
		return this.endCount;
	}

	@Override
	public boolean setEndCount (final long endCount) {
		if (this.endCount != endCount) {
			this.endCount = endCount;
			return true;
		}
		return false;
	}

	@Override
	public Date getDateLastPlayed () {
		return this.dateLastPlayed;
	}

	@Override
	public boolean setDateLastPlayed (final Date dateLastPlayed) {
		if (!Objects.equals(this.dateLastPlayed, dateLastPlayed)) {
			this.dateLastPlayed = dateLastPlayed;
			return true;
		}
		return false;
	}

	@Override
	public File findCoverArt () {
		return CoverArtHelper.findCoverArt(this);
	}

	@Override
	public String getCoverArtRemoteLocation () {
		return null;
	}

	@Override
	public boolean isPicture () {
		return (getMediaType() == MediaType.PICTURE);
	}

	private int width;
	private int height;

	@Override
	public int getWidth () {
		return this.width;
	}

	@Override
	public boolean setWidth (final int width) {
		if (this.width != width) {
			this.width = width;
			return true;
		}
		return false;
	}

	@Override
	public int getHeight () {
		return this.height;
	}

	@Override
	public boolean setHeight (final int height) {
		if (this.height != height) {
			this.height = height;
			return true;
		}
		return false;
	}

	//	Mass setter.

	@Override
	public void reset() {
		this.setDateAdded(null);
		this.setMd5(MD5_DEFAULT);
		this.setSha1(SHA1_DEFAULT);
		this.setDateLastModified(null);
		this.setEnabled(ENABLED_DEFAULT, null);
		this.setMissing(MISSING_DEFAULT);
		this.setDbRowId(DBROWID_DEFAULT);
		this.setRemoteLocation(null);

		this.setDuration(0);
		this.setStartCount(0);
		this.setEndCount(0);
		this.setDateLastPlayed(null);
		this.setWidth(0);
		this.setHeight(0);
	}

	@Override
	public boolean setFromMediaItem(final IMediaItem mi) {
		boolean b = this.setFilepathIfNotSet(mi.getFilepath()) // Do not set it if it is already set.
				| this.setDateAdded(mi.getDateAdded())
				| this.setMd5(mi.getMd5())
				| this.setSha1(mi.getSha1())
				| this.setDateLastModified(mi.getDateLastModified())
				| this.setEnabled(mi.isEnabled(), mi.enabledLastModified())
				| this.setMissing(mi.isMissing())
				| this.setDbRowId(mi.getDbRowId())
				| this.setRemoteLocation(mi.getRemoteLocation())
				| this.setDuration(mi.getDuration())
				| this.setStartCount(mi.getStartCount())
				| this.setEndCount(mi.getEndCount())
				| this.setDateLastPlayed(mi.getDateLastPlayed())
				| this.setWidth(mi.getWidth())
				| this.setHeight(mi.getHeight())
				| this.setMediaType(mi.getMediaType());
		return b;
	}

	@Override
	public boolean equals(final Object aThat) {
		if (aThat == null) return false;
		if (this == aThat) return true;
		if (!(aThat instanceof DefaultMediaItem)) return false;
		final DefaultMediaItem that = (DefaultMediaItem) aThat;

		return Objects.equals(getFilepath(), that.getFilepath());  // FIXME what if filepath is null?
	}

	@Override
	public int hashCode() {
		return getFilepath().hashCode();  // FIXME what if filepath is null?
	}

	@Override
	public String toString() {
		return getTitle();
	}

}

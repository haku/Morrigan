package morrigan.model.media;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import morrigan.model.db.IDbItem;
import morrigan.model.exceptions.MorriganException;



public interface MediaItem extends AbstractItem, IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * File size in bytes.
	 */
	long getFileSize ();

	String getFilepath ();
	boolean setFilepath (String filePath);
	File getFile ();

	/**
	 * May return null.
	 */
	boolean hasMimeType();
	String getMimeType();
	void setMimeType(String newType);

	Date getDateAdded();
	boolean setDateAdded(Date dateAdded);

	BigInteger getMd5();
	boolean setMd5(BigInteger md5);

	BigInteger getSha1();
	boolean setSha1(BigInteger sha1);

	Date getDateLastModified();
	boolean setDateLastModified(Date lastModified);

	boolean isEnabled();
	/**
	 * May return null.
	 */
	Date enabledLastModified();
	boolean setEnabled(boolean enabled);
	boolean setEnabled(boolean enabled, Date lastModified);

	boolean isMissing();
	boolean setMissing(boolean missing);

	boolean hasRemoteLocation();
	String getRemoteLocation();
	boolean setRemoteLocation(String remoteLocation);

	/**
	 * matches MediaList's hasFile() and getByFile().
	 * never null.
	 */
	default String getId() {
		return StringUtils.firstNonBlank(getRemoteId(), getFilepath());
	}

	/**
	 * May return null.
	 */
	String getRemoteId ();

	List<MediaTag> getTags() throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// track

	boolean isPlayable ();

	/**
	 * In seconds.
	 */
	int getDuration();
	boolean setDuration(int duration);

	long getStartCount();
	boolean setStartCount(long startCount);

	long getEndCount();
	boolean setEndCount(long endCount);

	Date getDateLastPlayed();
	boolean setDateLastPlayed(Date dateLastPlayed);

	/**
	 * @return File path to cover art or null.
	 */
	File findCoverArt();

	/**
	 * May return null;
	 */
	String getCoverArtRemoteLocation();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// picture

	boolean isPicture ();

	int getWidth();
	boolean setWidth(int width);

	int getHeight();
	boolean setHeight(int height);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// mixed

	//	Fixed enums - changing these requires writing more code.
	static enum MediaType {
		UNKNOWN(0, "all"), TRACK(1, "tracks"), PICTURE(2, "pictures");

		private final String humanName;
		private final int n;

		MediaType(int n, String humanName) {
			this.n = n;
			this.humanName = humanName;
		}

		public int getN() {
			return this.n;
		}

		public String getHumanName () {
			return this.humanName;
		}

		@Override
		public String toString() {
			return getHumanName();
		}

		public static MediaType parseInt (int n) {
			switch (n) {
				case 0: return UNKNOWN;
				case 1: return TRACK;
				case 2: return PICTURE;
				default: throw new IllegalArgumentException();
			}
		}

	}

	MediaType getMediaType ();
	boolean setMediaType (MediaType newType);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Reset item back to its default state.
	 * This may not reset fixed properties such as filename.
	 */
	void reset ();

	boolean setFromMediaItem (MediaItem mt);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	String getTitle ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

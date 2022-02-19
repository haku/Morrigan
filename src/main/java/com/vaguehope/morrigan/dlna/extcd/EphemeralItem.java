package com.vaguehope.morrigan.dlna.extcd;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaPicture;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.util.Objs;

public abstract class EphemeralItem implements IMixedMediaItem {

	public EphemeralItem () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public int hashCode () {
		return Objs.hash(getRemoteId());
	}

	@Override
	public boolean equals (final Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (!(obj instanceof EphemeralItem)) return false;
		final EphemeralItem that = (EphemeralItem) obj;
		return Objs.equals(getRemoteId(), that.getRemoteId());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Might later be variable metadata.

	@Override
	public long getStartCount () {
		return 0;
	}

	@Override
	public long getEndCount () {
		return 0;
	}

	@Override
	public Date getDateLastPlayed () {
		return null;
	}

	@Override
	public Date getDateLastModified () {
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Fixed metadata.

	@Override
	public String getFilepath () {
		return null;
	}

	@Override
	public File getFile () {
		return null;
	}

	@Override
	public File findCoverArt () {
		return null;
	}

	@Override
	public boolean isEnabled () {
		return true;
	}

	@Override
	public Date enabledLastModified () {
		return null;
	}

	@Override
	public boolean isMissing () {
		return false;
	}

	@Override
	public BigInteger getHashcode () {
		return BigInteger.ZERO;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Unsupported metadata.

	@Override
	public long getDbRowId () {
		throw new UnsupportedOperationException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters.

	@Override
	public void reset () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setDuration (final int duration) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setStartCount (final long startCount) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setEndCount (final long endCount) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setDateLastPlayed (final Date dateLastPlayed) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setFromMediaTrack (final IMediaTrack mt) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setFilepath (final String filePath) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setDateAdded (final Date dateAdded) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setHashcode (final BigInteger hashcode) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setDateLastModified (final Date lastModified) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setEnabled (final boolean enabled) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setEnabled (final boolean enabled, final Date lastModified) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setMissing (final boolean missing) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setRemoteLocation (final String remoteLocation) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setFromMediaItem (final IMediaItem mt) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setDbRowId (final long dbRowId) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setWidth (final int width) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setHeight (final int height) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setFromMediaPicture (final IMediaPicture mp) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setMediaType (final MediaType newType) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean setFromMediaMixedItem (final IMixedMediaItem mmi) {
		throw new UnsupportedOperationException("Not implemented.");
	}

}

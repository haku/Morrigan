package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.util.Date;
import java.util.Objects;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.internal.CoverArtHelper;
import com.vaguehope.morrigan.model.media.internal.MediaItem;

public class MixedMediaItem extends MediaItem implements IMediaItem {

	protected MixedMediaItem (final String filePath, final IMediaItemList list) {
		super(filePath, list);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.

//	-  -  -  -  -  -  -  -  -
//	IMixedMediaItem

	private MediaType type;

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

//	-  -  -  -  -  -  -  -  -
//	IMediaTrack.

	@Override
	public boolean isPlayable () {
		return (getMediaType() == MediaType.TRACK);
	}

	private int duration;
	private long startCount;
	private long endCount;
	private Date dateLastPlayed;

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

//	-  -  -  -  -  -  -  -  -
//	IMediaPicture.

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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.

	@Override
	public void reset () {
		super.reset();

		this.setDuration(0);
		this.setStartCount(0);
		this.setEndCount(0);
		this.setDateLastPlayed(null);
		this.setWidth(0);
		this.setHeight(0);
	}

	private boolean _setFromMediaTrack (final IMediaItem mt) {
		boolean b =
				this.setDuration(mt.getDuration())
						| this.setStartCount(mt.getStartCount())
						| this.setEndCount(mt.getEndCount())
						| this.setDateLastPlayed(mt.getDateLastPlayed());
		return b;
	}

	@Override
	public boolean setFromMediaTrack (final IMediaItem mt) {
		boolean b =
				this.setFromMediaItem(mt)
						| _setFromMediaTrack(mt);
		return b;
	}

	private boolean _setFromMediaPicture (final IMediaItem mp) {
		boolean b =
				this.setWidth(mp.getWidth())
						| this.setHeight(mp.getHeight());
		return b;
	}

	@Override
	public boolean setFromMediaPicture (final IMediaItem mp) {
		boolean b =
				this.setFromMediaItem(mp)
						| _setFromMediaPicture(mp);
		return b;
	}

	@Override
	public boolean setFromMediaMixedItem (final IMediaItem mmi) {
		boolean b =
				this.setFromMediaItem(mmi)
						| _setFromMediaTrack(mmi)
						| _setFromMediaPicture(mmi)
						| this.setMediaType(mmi.getMediaType());
		return b;
	}

	@Override
	public boolean setFromMediaItem (final IMediaItem mi) {
		boolean ret = super.setFromMediaItem(mi);

		if (mi instanceof IMediaItem) {
			IMediaItem mmi = mi;

			boolean b =
					this.setMediaType(mmi.getMediaType());

			ret = b | ret;
		}

		if (mi instanceof IMediaItem) {
			IMediaItem mt = mi;

			boolean b = this.setDuration(mt.getDuration())
					| this.setStartCount(mt.getStartCount())
					| this.setEndCount(mt.getEndCount())
					| this.setDateLastPlayed(mt.getDateLastPlayed());

			ret = b | ret;
		}

		if (mi instanceof IMediaItem) {
			IMediaItem mli = mi;

			boolean b = this.setWidth(mli.getWidth())
					| this.setHeight(mli.getHeight());

			ret = b | ret;
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

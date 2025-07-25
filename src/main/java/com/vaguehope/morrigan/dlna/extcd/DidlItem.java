package com.vaguehope.morrigan.dlna.extcd;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jupnp.model.ModelUtil;
import org.jupnp.support.model.DIDLObject;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.EphemeralItem;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.util.StringHelper;

public class DidlItem extends EphemeralItem {

	private static final Logger LOG = LoggerFactory.getLogger(DidlItem.class);

	private final Item item;
	private final Res primaryRes;
	private final MediaType mediaType;
	private final Res artRes;
	private final Metadata metadata;
	private final int durationSeconds;
	private final Date dateAdded;

	public DidlItem (final Item item, final Res primaryRes, final MediaType mediaType, final Res artRes, final Metadata metadata) {
		this.item = item;
		this.primaryRes = primaryRes;
		this.mediaType = mediaType;
		this.artRes = artRes;
		this.metadata = metadata;
		this.durationSeconds = (int) (StringHelper.notBlank(primaryRes.getDuration()) ? ModelUtil.fromTimeString(primaryRes.getDuration()) : 0);
		this.dateAdded = findDate(item);
	}

	private static Date findDate (final Item item) {
		final String dateProp = item.getFirstPropertyValue(DIDLObject.Property.DC.DATE.class);
		if (dateProp != null) {
			try {
				return UpnpHelper.DC_DATE_FORMAT.get().parse(dateProp);
			}
			catch (final ParseException e) {
				LOG.warn("Invalid DC:DATE: " + dateProp);
			}
		}
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Resource access.

	/**
	 * Used by ContentDirectoryDb's hasFile() and getByFile().
	 */
	@Override
	public String getRemoteId () {
		return this.item.getId();
	}

	@Override
	public long getFileSize () {
		final Long size = this.primaryRes.getSize();
		if (size == null) return 0;
		return size;
	}

	@Override
	public String getMimeType () {
		return this.primaryRes.getProtocolInfo().getContentFormat();
	}

	@Override
	public boolean hasRemoteLocation() {
		return true;
	}

	@Override
	public String getRemoteLocation () {
		return this.primaryRes.getValue();
	}

	@Override
	public String getCoverArtRemoteLocation () {
		if (this.artRes == null) return null;
		return this.artRes.getValue();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Item metadata.

	@Override
	public String getTitle () {
		return this.item.getTitle();
	}

	@Override
	public String toString() {
		return getTitle();
	}

	@Override
	public boolean isPlayable () {
		return this.mediaType == MediaType.TRACK;
	}

	@Override
	public int getDuration () {
		return this.durationSeconds;
	}

	@Override
	public Date getDateAdded () {
		return this.dateAdded;
	}

	@Override
	public MediaType getMediaType () {
		return this.mediaType;
	}

	@Override
	public boolean isPicture () {
		return this.mediaType == MediaType.PICTURE;
	}

	@Override
	public int getWidth () {
		return this.primaryRes.getResolutionX();
	}

	@Override
	public int getHeight () {
		return this.primaryRes.getResolutionY();
	}

	@Override
	public List<MediaTag> getTags() throws MorriganException {
		return Collections.emptyList();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Persisted metadata.

	@Override
	public Date getDateLastPlayed () {
		return this.metadata.getDateLastPlayed();
	}

	@Override
	public long getStartCount () {
		return this.metadata.getStartCount();
	}

	@Override
	public long getEndCount () {
		return this.metadata.getEndCount();
	}


}

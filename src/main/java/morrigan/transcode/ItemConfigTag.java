package morrigan.transcode;

import java.util.Date;

import morrigan.model.media.MediaTag;
import morrigan.util.MnLogger;
import morrigan.util.StringHelper;
import morrigan.util.TimeHelper;

public class ItemConfigTag {

	private static final MnLogger LOG = MnLogger.make(ItemConfigTag.class);

	private final ConfigTag configTag;
	private final MediaTag mediaTag;

	public ItemConfigTag (final ConfigTag configTag, final MediaTag mediaTag) {
		this.configTag = configTag;
		this.mediaTag = mediaTag;
	}

	public Date getLastModified() {
		return this.mediaTag.getModified();
	}

	public boolean isPresent() {
		return !this.mediaTag.isDeleted();
	}

	public String parseAsString() {
		if (this.mediaTag.isDeleted()) return null;

		return StringHelper.trimToNull(
				this.mediaTag.getTag().substring(this.configTag.getPrefix().length()));
	}

	public Long parseAsDuration () {
		if (this.mediaTag.isDeleted()) return null;

		final String rawVal = StringHelper.trimToNull(
				this.mediaTag.getTag().substring(this.configTag.getPrefix().length()));
		if (rawVal == null) return null;

		final Long val = TimeHelper.parseDuration(rawVal);
		if (val != null) return val;

		LOG.w("Invalid trim_end: {}", rawVal);
		return null;
	}

}

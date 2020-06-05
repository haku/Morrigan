package com.vaguehope.morrigan.transcode;

import java.util.Date;

import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.TimeHelper;

public class ItemConfigTag {

	private static final MnLogger LOG = MnLogger.make(TranscodeProfile.class);

	private final ConfigTag configTag;
	private final MediaTag mediaTag;

	public ItemConfigTag (final ConfigTag configTag, final MediaTag mediaTag) {
		this.configTag = configTag;
		this.mediaTag = mediaTag;
	}

	public Date getLastModified() {
		return this.mediaTag.getModified();
	}

	public Long parseAsDuration () {
		if (this.mediaTag.isDeleted()) return null;

		final String rawVal = this.mediaTag.getTag().substring(this.configTag.getPrefix().length());
		if (StringHelper.blank(rawVal)) return null;

		final Long val = TimeHelper.parseDuration(rawVal);
		if (val != null) return val;

		LOG.w("Invalid trim_end: {}", rawVal);
		return null;
	}

}

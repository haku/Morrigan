package com.vaguehope.morrigan.dlna.content;

import org.seamless.util.MimeType;

public enum ContentGroup {

	ROOT("0", "-", "Root"), // Root id of '0' is in the spec.
	VIDEO("1-videos", "video-", "Videos"),
	IMAGE("2-images", "image-", "Images"),
	AUDIO("3-audio", "audio-", "Audio");

	private final String id;
	private final String itemIdPrefix;
	private final String humanName;

	private ContentGroup (final String id, final String itemIdPrefix, final String humanName) {
		this.id = id;
		this.itemIdPrefix = itemIdPrefix;
		this.humanName = humanName;
	}

	public String getId () {
		return this.id;
	}

	public String getItemIdPrefix () {
		return this.itemIdPrefix;
	}

	public String getHumanName () {
		return this.humanName;
	}

	public static ContentGroup fromMimeType (final MimeType mimeType) {
		if ("video".equalsIgnoreCase(mimeType.getType())) {
			return VIDEO;
		}
		else if ("audio".equalsIgnoreCase(mimeType.getType())) {
			return AUDIO;
		}
		else if ("image".equalsIgnoreCase(mimeType.getType())) {
			return IMAGE;
		}
		return ROOT;
	}

}

package com.vaguehope.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;

public class MediaTagClassificationFactory extends RecyclingFactory<MediaTagClassificationImpl, Long, String, RuntimeException> {

	public MediaTagClassificationFactory() {
		super(true);
	}

	@Override
	protected boolean isValidProduct(final MediaTagClassificationImpl product) {
		return true;
	}

	@Override
	protected MediaTagClassificationImpl makeNewProduct(final Long material, final String config) {
		return new MediaTagClassificationImpl(material, config);
	}

}

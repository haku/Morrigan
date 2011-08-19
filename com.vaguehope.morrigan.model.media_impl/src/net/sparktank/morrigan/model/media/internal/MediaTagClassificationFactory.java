package net.sparktank.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;

public class MediaTagClassificationFactory extends RecyclingFactory<MediaTagClassificationImpl, Long, String, RuntimeException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public final MediaTagClassificationFactory INSTANCE = new MediaTagClassificationFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MediaTagClassificationFactory() {
		super(true);
	}
	
	@Override
	protected boolean isValidProduct(MediaTagClassificationImpl product) {
		return true;
	}
	
	@SuppressWarnings("boxing")
	@Override
	protected MediaTagClassificationImpl makeNewProduct(Long material, String config) {
		return new MediaTagClassificationImpl(material, config);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

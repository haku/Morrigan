package net.sparktank.morrigan.model.tags;

import net.sparktank.morrigan.model.factory.RecyclingFactory;

public class MediaTagClassificationFactory extends RecyclingFactory<MediaTagClassification, Long, String, RuntimeException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public final MediaTagClassificationFactory INSTANCE = new MediaTagClassificationFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MediaTagClassificationFactory() {
		super(true);
	}
	
	@Override
	protected boolean isValidProduct(MediaTagClassification product) {
		return true;
	}
	
	@SuppressWarnings("boxing")
	@Override
	protected MediaTagClassification makeNewProduct(Long material, String config) {
		return new MediaTagClassification(material, config);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

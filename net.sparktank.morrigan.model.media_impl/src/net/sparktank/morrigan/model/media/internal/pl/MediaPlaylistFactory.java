package net.sparktank.morrigan.model.media.internal.pl;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.factory.RecyclingFactory;

public class MediaPlaylistFactory extends RecyclingFactory<MediaPlaylist, String, Boolean, MorriganException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final MediaPlaylistFactory INSTANCE = new MediaPlaylistFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaPlaylistFactory() {
		super(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected boolean isValidProduct(MediaPlaylist product) {
		System.out.println("Found '" + product.getFilePath() + "' in cache.");
		return true;
	}
	
	@SuppressWarnings("boxing")
	@Override
	protected MediaPlaylist makeNewProduct(String material) throws MorriganException {
		return makeNewProduct(material, false);
	}
	
	@SuppressWarnings("boxing")
	@Override
	protected MediaPlaylist makeNewProduct(String material, Boolean config) throws MorriganException {
		MediaPlaylist ret = null;
		
		System.out.println("Making object instance '" + material + "'...");
		ret = new MediaPlaylist(PlaylistHelper.getPlaylistTitle(material), material, config);
		
		return ret;
	}
	
	@Override
	protected void disposeProduct(MediaPlaylist product) {
		try {
			product.clean();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
package net.sparktank.morrigan.model.tracks;
import java.net.MalformedURLException;
import java.net.URL;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.library.SqliteLayer;
import net.sparktank.morrigan.model.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.library.local.LocalMediaLibrary;
import net.sparktank.morrigan.model.library.remote.RemoteLibraryHelper;
import net.sparktank.morrigan.model.library.remote.RemoteMediaLibrary;
import net.sparktank.morrigan.model.tracks.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.tracks.playlist.PlaylistHelper;
import net.sparktank.sqlitewrapper.DbException;

public class MediaTrackListFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static class LocalMediaLibraryFactory extends RecyclingFactory<LocalMediaLibrary, String, Void, DbException> {
		
		protected LocalMediaLibraryFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(LocalMediaLibrary product) {
			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected LocalMediaLibrary makeNewProduct(String material) throws DbException {
			System.out.println("Making object instance '" + material + "'...");
			return new LocalMediaLibrary(LocalLibraryHelper.getLibraryTitle(material), SqliteLayer.FACTORY.manufacture(material));
		}
		
	}
	
	public static final LocalMediaLibraryFactory LOCAL_MEDIA_LIBRARY_FACTORY = new LocalMediaLibraryFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static class RemoteMediaLibraryFactory extends RecyclingFactory<RemoteMediaLibrary, String, URL, MorriganException> {
		
		protected RemoteMediaLibraryFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(RemoteMediaLibrary product) {
			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected RemoteMediaLibrary makeNewProduct(String material) throws MorriganException {
			return makeNewProduct(material, null);
		}
		
		@Override
		protected RemoteMediaLibrary makeNewProduct(String material, URL config) throws MorriganException {
			RemoteMediaLibrary ret = null;
			
			System.out.println("Making object instance '" + material + "'...");
			if (config != null) {
				try {
					ret = new RemoteMediaLibrary(RemoteLibraryHelper.getLibraryTitle(material), config, SqliteLayer.FACTORY.manufacture(material));
				} catch (DbException e) {
					throw new MorriganException(e);
				}
			} else {
				try {
					ret = new RemoteMediaLibrary(RemoteLibraryHelper.getLibraryTitle(material), SqliteLayer.FACTORY.manufacture(material));
				} catch (MalformedURLException e) {
					throw new MorriganException(e);
				} catch (DbException e) {
					throw new MorriganException(e);
				}
			}
			
			return ret;
		}
		
	}
	
	public static final RemoteMediaLibraryFactory REMOTE_MEDIA_LIBRARY_FACTORY = new RemoteMediaLibraryFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static class PlaylistFactory extends RecyclingFactory<MediaPlaylist, String, Boolean, MorriganException> {
		
		protected PlaylistFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(MediaPlaylist product) {
			System.out.println("Found '" + product.getFilePath() + "' in cache.");
			return true;
		}
		
		@Override
		protected MediaPlaylist makeNewProduct(String material) throws MorriganException {
			return makeNewProduct(material, false);
		}
		
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
		
	}
	
	public static final PlaylistFactory PLAYLIST_FACTORY = new PlaylistFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

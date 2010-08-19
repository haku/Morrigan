package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.pictures.gallery.LocalGallery;
import net.sparktank.morrigan.model.tracks.library.local.LocalMediaLibrary;
import net.sparktank.morrigan.model.tracks.library.remote.RemoteMediaLibrary;
import net.sparktank.morrigan.model.tracks.playlist.MediaPlaylist;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class EditorFactory implements IElementFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.EditorFactory";
	
	public static final String KEY_TYPE = "TYPE";
	public static final String KEY_SERIAL = "SERIAL";
	public static final String KEY_TOPINDEX = "TOPINDEX";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO work around an eclipse bug by making it safe
	 * for this method to be called multiple times.
	 * (issues with restoring navigation history)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		MediaItemListEditorInput<?> input = null;
		
		String type = memento.getString(KEY_TYPE);
		
		try {
			if (type.equals(LocalMediaLibrary.TYPE)) {
				input = getMediaLibraryInput(memento);
			}
			else if (type.equals(MediaPlaylist.TYPE)) {
				String serial = memento.getString(KEY_SERIAL);
				input = getMediaPlaylistInput(serial);
			}
			else if (type.equals(RemoteMediaLibrary.TYPE)) {
				input = getRemoteMediaLibraryInput(memento);
			}
			else if (type.equals(LocalGallery.TYPE)) {
				input = getGalleryInput(memento);
			}
			else if (type.equals(LocalMixedMediaDb.TYPE)) {
				input = getMmdbInput(memento);
			}
			else {
				System.err.println("EditorFactory.createElement(): Unknown type: '"+type+"'.");
				return null;
			}
			
		} catch (MorriganException e) {
			e.printStackTrace();
		}
		
		String topIndex = memento.getString(KEY_TOPINDEX);
		if (topIndex != null && input!= null) {
			int i = Integer.parseInt(topIndex);
			input.setTopIndex(i);
		}
		
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaItemDbEditorInput getMediaLibraryInput (String dbFilePath) throws MorriganException {
		LocalMediaLibrary ml;
		
		try {
			ml = LocalMediaLibrary.FACTORY.manufacture(dbFilePath);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		MediaItemDbEditorInput input = new MediaItemDbEditorInput(ml);
		return input;
	}
	
	public static MediaItemDbEditorInput getMediaLibraryInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		MediaItemDbEditorInput input = getMediaLibraryInput(dbFilePath);
		return input;
	}
	
	public static MediaItemDbEditorInput getRemoteMediaLibraryInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		MediaItemDbEditorInput input = getRemoteMediaLibraryInput(dbFilePath);
		return input;
	}
	
	public static MediaItemDbEditorInput getRemoteMediaLibraryInput (String dbFilePath) throws MorriganException {
		RemoteMediaLibrary ml = RemoteMediaLibrary.FACTORY.manufacture(dbFilePath);
		MediaItemDbEditorInput input = new MediaItemDbEditorInput(ml);
		return input;
	}
	
	public static MediaItemListEditorInput<MediaPlaylist> getMediaPlaylistInput (String filePath) throws MorriganException {
		MediaPlaylist playList;
		try {
			playList = MediaPlaylist.FACTORY.manufacture(filePath);
		} catch (MorriganException e) {
			throw new MorriganException(e);
		}
		
		MediaItemListEditorInput<MediaPlaylist> input = new MediaItemListEditorInput<MediaPlaylist>(playList);
		return input;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local Galleries.
	
	public static MediaItemDbEditorInput getGalleryInput(String dbFilePath) throws MorriganException {
		LocalGallery l;
		
		try {
			l = LocalGallery.FACTORY.manufacture(dbFilePath);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		MediaItemDbEditorInput input = new MediaItemDbEditorInput(l);
		return input;
	}
	
	public static MediaItemDbEditorInput getGalleryInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		MediaItemDbEditorInput input = getGalleryInput(dbFilePath);
		return input;
	}
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local MixedMediaDb.
	
	public static MediaItemDbEditorInput getMmdbInput(String dbFilePath) throws MorriganException {
		LocalMixedMediaDb l;
		
		try {
			l = LocalMixedMediaDb.LOCAL_MMDB_FACTORY.manufacture(dbFilePath);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		MediaItemDbEditorInput input = new MediaItemDbEditorInput(l);
		return input;
	}
	
	public static MediaItemDbEditorInput getMmdbInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		MediaItemDbEditorInput input = getMmdbInput(dbFilePath);
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

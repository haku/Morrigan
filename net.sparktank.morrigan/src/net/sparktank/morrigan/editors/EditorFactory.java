package net.sparktank.morrigan.editors;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.media.MediaListFactory;
import net.sparktank.morrigan.model.media.MediaPlaylist;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class EditorFactory implements IElementFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.editors.EditorFactory";
	
	public static final String KEY_TYPE = "TYPE";
	public static final String KEY_SERIAL = "SERIAL";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IAdaptable createElement(IMemento memento) {
		String type = memento.getString(KEY_TYPE);
		String serial = memento.getString(KEY_SERIAL);
		
		System.out.println("restore=" + type + ":" + serial);
		
		try {
			if (type.equals(MediaLibrary.TYPE)) {
				return getMediaLibraryInput(serial);
				
			} else if (type.equals(MediaPlaylist.TYPE)) {
				return getMediaPlaylistInput(serial);
			}
			
		} catch (MorriganException e) {
			e.printStackTrace();
			return null;
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaListEditorInput<MediaLibrary> getMediaLibraryInput (String dbFilePath) throws MorriganException {
		MediaLibrary ml;
		try {
			ml = MediaListFactory.makeMediaLibrary(dbFilePath);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		MediaListEditorInput<MediaLibrary> input = new MediaListEditorInput<MediaLibrary>(ml);
		return input;
	}
	
	public static MediaListEditorInput<MediaPlaylist> getMediaPlaylistInput (String filePath) throws MorriganException {
		MediaPlaylist playList;
		try {
			playList = MediaListFactory.makeMediaPlaylist(filePath);
		} catch (MorriganException e) {
			throw new MorriganException(e);
		}
		
		MediaListEditorInput<MediaPlaylist> input = new MediaListEditorInput<MediaPlaylist>(playList);
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

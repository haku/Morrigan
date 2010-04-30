package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.library.SqliteLayer.*;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class EditorFactory implements IElementFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.EditorFactory";
	
	public static final String KEY_TYPE = "TYPE";
	public static final String KEY_SERIAL = "SERIAL";
	public static final String KEY_TOPINDEX = "TOPINDEX";
	
	public static final String KEY_LIB_SORTCOL = "LIB_SORTCOL";
	public static final String KEY_LIB_SORTDIR = "LIB_SORTDIR";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IAdaptable createElement(IMemento memento) {
		MediaListEditorInput<?> input = null;
		
		String type = memento.getString(KEY_TYPE);
		
		try {
			if (type.equals(MediaLibrary.TYPE)) {
				input = getMediaLibraryInput(memento);
				
			} else if (type.equals(MediaPlaylist.TYPE)) {
				String serial = memento.getString(KEY_SERIAL);
				input = getMediaPlaylistInput(serial);
			}
			
		} catch (MorriganException e) {
			e.printStackTrace();
		}
		
		String topIndex = memento.getString(KEY_TOPINDEX);
		if (topIndex != null) {
			int i = Integer.parseInt(topIndex);
			input.setTopIndex(i);
		}
		
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaListEditorInput<MediaLibrary> getMediaLibraryInput (String dbFilePath) throws MorriganException {
		MediaLibrary ml;
		
		try {
			ml = MediaListFactory.makeMediaLibrary(dbFilePath);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		LibraryEditorInput input = new LibraryEditorInput(ml);
		return input;
	}
	
	public static MediaListEditorInput<MediaLibrary> getMediaLibraryInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		MediaListEditorInput<MediaLibrary> input = getMediaLibraryInput(dbFilePath);
		
		String sortcol = memento.getString(KEY_LIB_SORTCOL);
		String sortdir = memento.getString(KEY_LIB_SORTDIR);
		if (sortcol != null && sortdir != null) {
			try {
				LibrarySort ls = LibrarySort.valueOf(sortcol);
				LibrarySortDirection lsd = LibrarySortDirection.valueOf(sortdir);
				input.getMediaList().setSort(ls, lsd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
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

package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySortDirection;
import net.sparktank.morrigan.model.library.local.LocalMediaLibrary;
import net.sparktank.morrigan.model.library.remote.RemoteMediaLibrary;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;
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
	
	public static final String KEY_LIB_SORTCOL = "LIB_SORTCOL";
	public static final String KEY_LIB_SORTDIR = "LIB_SORTDIR";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO work around an eclipse bug by making it safe
	 * for this method to be called multiple times.
	 * (issues with restoring navigation history)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		MediaTrackListEditorInput<?> input = null;
		
		String type = memento.getString(KEY_TYPE);
		
		try {
			if (type.equals(LocalMediaLibrary.TYPE)) {
				input = getMediaLibraryInput(memento);
				
			} else if (type.equals(MediaPlaylist.TYPE)) {
				String serial = memento.getString(KEY_SERIAL);
				input = getMediaPlaylistInput(serial);
				
			} else if (type.equals(RemoteMediaLibrary.TYPE)) {
				input = getRemoteMediaLibraryInput(memento);
				
			} else {
				throw new IllegalArgumentException("Unknown type: '"+type+"'.");
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
	
	public static LibraryEditorInput getMediaLibraryInput (String dbFilePath) throws MorriganException {
		LocalMediaLibrary ml;
		
		try {
			ml = MediaListFactory.LOCAL_MEDIA_LIBRARY_FACTORY.manufacture(dbFilePath);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		
		LibraryEditorInput input = new LibraryEditorInput(ml);
		return input;
	}
	
	public static LibraryEditorInput getMediaLibraryInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		LibraryEditorInput input = getMediaLibraryInput(dbFilePath);
		
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
	
	public static LibraryEditorInput getRemoteMediaLibraryInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		LibraryEditorInput input = getRemoteMediaLibraryInput(dbFilePath);
		
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
	
	public static LibraryEditorInput getRemoteMediaLibraryInput (String dbFilePath) throws MorriganException {
		RemoteMediaLibrary ml = MediaListFactory.REMOTE_MEDIA_LIBRARY_FACTORY.manufacture(dbFilePath);
		LibraryEditorInput input = new LibraryEditorInput(ml);
		return input;
	}
	
	public static MediaTrackListEditorInput<MediaPlaylist> getMediaPlaylistInput (String filePath) throws MorriganException {
		MediaPlaylist playList;
		try {
			playList = MediaListFactory.PLAYLIST_FACTORY.manufacture(filePath);
		} catch (MorriganException e) {
			throw new MorriganException(e);
		}
		
		MediaTrackListEditorInput<MediaPlaylist> input = new MediaTrackListEditorInput<MediaPlaylist>(playList);
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

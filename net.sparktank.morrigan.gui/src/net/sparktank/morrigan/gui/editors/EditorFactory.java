package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaPlaylist;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;
import net.sparktank.morrigan.server.model.RemoteMixedMediaDb;
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
			if (type.equals(ILocalMixedMediaDb.TYPE)) {
				input = getMmdbInput(memento);
			}
			else if (type.equals(IRemoteMixedMediaDb.TYPE)) {
				input = getRemoteMmdbInput(memento);
			}
			else if (type.equals(IMediaPlaylist.TYPE)) {
				String serial = memento.getString(KEY_SERIAL);
				input = getMediaPlaylistInput(serial);
			}
			else {
				System.err.println("EditorFactory.createElement(): Unknown type: '"+type+"'.");
				return null;
			}
		}
		catch (MorriganException e) {
			e.printStackTrace();
		}
		
		String topIndex = memento.getString(KEY_TOPINDEX);
		if (topIndex != null && input!= null) {
			int i = Integer.parseInt(topIndex);
			input.setTopIndex(i);
		}
		
		return input;
	}
	
//	- - - - - - - - - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
//	Playlists.
	
	public static MediaItemListEditorInput<IMediaPlaylist> getMediaPlaylistInput (String filePath) throws MorriganException {
		IMediaPlaylist playList;
		try {
			playList = MediaFactoryImpl.get().getPlaylist(filePath);
		} catch (MorriganException e) {
			throw new MorriganException(e);
		}
		
		MediaItemListEditorInput<IMediaPlaylist> input = new MediaItemListEditorInput<IMediaPlaylist>(playList);
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local MixedMediaDb.
	
	public static MediaItemDbEditorInput getMmdbInput(String dbFilePath) throws MorriganException {
		ILocalMixedMediaDb l;
		
		try {
			l = MediaFactoryImpl.get().getLocalMixedMediaDb(dbFilePath);
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
//	Remote MixedMediaDb.
	
	public static MediaItemDbEditorInput getRemoteMmdbInput (IMemento memento) throws MorriganException {
		String dbFilePath = memento.getString(KEY_SERIAL);
		MediaItemDbEditorInput input = getRemoteMmdbInput(dbFilePath);
		return input;
	}
	
	public static MediaItemDbEditorInput getRemoteMmdbInput (String dbFilePath) throws MorriganException {
		IRemoteMixedMediaDb ml = RemoteMixedMediaDb.FACTORY.manufacture(dbFilePath);
		MediaItemDbEditorInput input = new MediaItemDbEditorInput(ml);
		return input;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

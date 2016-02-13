package com.vaguehope.morrigan.gui.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.sqlitewrapper.DbException;

public class EditorFactory implements IElementFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String ID = "com.vaguehope.morrigan.gui.editors.EditorFactory";

	public static final String KEY_TYPE = "TYPE";
	public static final String KEY_SERIAL = "SERIAL";
	public static final String KEY_TOPINDEX = "TOPINDEX";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * TODO work around an eclipse bug by making it safe for this method to be
	 * called multiple times. (issues with restoring navigation history)
	 */
	@Override
	public IAdaptable createElement (final IMemento memento) {
		MediaItemListEditorInput<?> input = null;

		String type = memento.getString(KEY_TYPE);

		try {
			if (type.equals(MediaListType.LOCALMMDB.toString())) {
				input = getMmdbInput(memento);
			}
			else if (type.equals(MediaListType.REMOTEMMDB.toString())) {
				input = getRemoteMmdbInput(memento);
			}
			else {
				System.err.println("EditorFactory.createElement(): Unknown type: '" + type + "'.");
				return null;
			}
		}
		catch (MorriganException e) {
			e.printStackTrace();
		}

		String topIndex = memento.getString(KEY_TOPINDEX);
		if (topIndex != null && input != null) {
			int i = Integer.parseInt(topIndex);
			input.setTopIndex(i);
		}

		return input;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local MixedMediaDb.

	public static MediaItemDbEditorInput getMmdbInput (final String dbFilePath) throws MorriganException {
		return getMmdbInput(dbFilePath, null);
	}

	public static MediaItemDbEditorInput getMmdbInput (final String dbFilePath, final String filter) throws MorriganException {
		ILocalMixedMediaDb l;
		try {
			l = Activator.getMediaFactory().getLocalMixedMediaDb(dbFilePath, filter);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
		return new MediaItemDbEditorInput(l);
	}

	public static MediaItemDbEditorInput getMmdbInputBySerial (final String serial) throws MorriganException {
		ILocalMixedMediaDb l;
		try {
			l = Activator.getMediaFactory().getLocalMixedMediaDbBySerial(serial);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
		return new MediaItemDbEditorInput(l);
	}

	public static MediaItemDbEditorInput getMmdbInput (final IMemento memento) throws MorriganException {
		String serial = memento.getString(KEY_SERIAL);
		return getMmdbInputBySerial(serial);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Remote MixedMediaDb.

	public static MediaItemDbEditorInput getRemoteMmdbInput (final String dbFilePath) throws MorriganException {
		IRemoteMixedMediaDb ml = RemoteMixedMediaDbFactory.getExisting(dbFilePath);
		return new MediaItemDbEditorInput(ml);
	}

	public static MediaItemDbEditorInput getRemoteMmdbInput (final String dbFilePath, final String filter) throws MorriganException {
		IRemoteMixedMediaDb ml = RemoteMixedMediaDbFactory.getExisting(dbFilePath, filter);
		return new MediaItemDbEditorInput(ml);
	}

	public static MediaItemDbEditorInput getRemoteMmdbInputBySerial (final String serial) throws MorriganException {
		IRemoteMixedMediaDb ml = RemoteMixedMediaDbFactory.getExistingBySerial(serial);
		return new MediaItemDbEditorInput(ml);
	}

	public static MediaItemDbEditorInput getRemoteMmdbInput (final IMemento memento) throws MorriganException {
		String serial = memento.getString(KEY_SERIAL);
		return getRemoteMmdbInputBySerial(serial);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

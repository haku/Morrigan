package com.vaguehope.morrigan.gui.editors.tracks;

import java.util.LinkedList;
import java.util.List;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.handlers.IHandlerService;

import com.vaguehope.morrigan.gui.actions.AddToPlaylistAction;
import com.vaguehope.morrigan.gui.adaptors.CountsLblProv;
import com.vaguehope.morrigan.gui.adaptors.DateAddedLblProv;
import com.vaguehope.morrigan.gui.adaptors.DateLastModifiedLblProv;
import com.vaguehope.morrigan.gui.adaptors.DateLastPlayerLblProv;
import com.vaguehope.morrigan.gui.adaptors.DurationLblProv;
import com.vaguehope.morrigan.gui.adaptors.FileLblProv;
import com.vaguehope.morrigan.gui.adaptors.HashcodeLblProv;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.editors.MediaColumn;
import com.vaguehope.morrigan.gui.editors.MediaItemListEditor;
import com.vaguehope.morrigan.gui.handler.AddToQueue;
import com.vaguehope.morrigan.gui.preferences.MediaListPref;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;

public abstract class MediaTrackListEditor<T extends IMediaTrackList<S>, S extends IMediaTrack> extends MediaItemListEditor<T, S> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Column definitions.
	
	public final MediaColumn 
		COL_FILE =       new MediaColumn("file",        new ColumnWeightData(100),            new FileLblProv(this.getImageCache()) );
	public final MediaColumn 
		COL_COUNTS =     new MediaColumn("counts",      new ColumnPixelData( 70, true, true), new CountsLblProv(),             SWT.CENTER);
	public final MediaColumn 
		COL_ADDED =      new MediaColumn("added",       new ColumnPixelData(140, true, true), new DateAddedLblProv()           );
	public final MediaColumn 
		COL_LASTPLAYED = new MediaColumn("last played", new ColumnPixelData(140, true, true), new DateLastPlayerLblProv()      );
	public final MediaColumn 
		COL_HASH =       new MediaColumn("hash",        new ColumnPixelData( 90, true, true), new HashcodeLblProv(),           SWT.CENTER);
	public final MediaColumn 
		COL_MODIFIED =   new MediaColumn("modified",    new ColumnPixelData(140, true, true), new DateLastModifiedLblProv()    );
	public final MediaColumn 
		COL_DURATION =   new MediaColumn("duration",    new ColumnPixelData( 60, true, true), new DurationLblProv(),           SWT.RIGHT);
	
	public final MediaColumn[] COLS = new MediaColumn[] {
		this.COL_FILE,
		this.COL_COUNTS,
		this.COL_ADDED,
		this.COL_LASTPLAYED,
		this.COL_HASH,
		this.COL_MODIFIED,
		this.COL_DURATION
	};
	
	@Override
	protected List<MediaColumn> getColumns() {
		List<MediaColumn> l = new LinkedList<MediaColumn>();
		for (MediaColumn c : this.COLS) {
			l.add(c);
		}
		return l;
	}
	
	@Override
	protected boolean isColumnVisible(MediaColumn col) {
		return MediaListPref.getColPref(this, col);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Menus and Actions.
	
	protected MenuManager getAddToMenu () {
		final MenuManager menu = new MenuManager("Add to playlist...");
		
		menu.addMenuListener(new IMenuListener () {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IEditorReference[] editors = getEditorSite().getPage().getEditorReferences();
				for (final IEditorReference e : editors) {
					if (e.getId().equals(PlaylistEditor.ID)) {
						menu.add(new AddToPlaylistAction(MediaTrackListEditor.this, e));
					}
				}
				if (menu.getItems().length < 1) {
					Action a = new Action("(No playlists open)") {/* UNUSED */};
					a.setEnabled(false);
					menu.add(a);
				}
			}
		});
		
		menu.setRemoveAllWhenShown(true);
		
		return menu;
	}
	
	protected IAction addToQueueAction = new Action("Enqueue") {
		@Override
		public void run() {
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			try {
				handlerService.executeCommand(AddToQueue.ID, null);
			} catch (Throwable t) {
				new MorriganMsgDlg(t).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

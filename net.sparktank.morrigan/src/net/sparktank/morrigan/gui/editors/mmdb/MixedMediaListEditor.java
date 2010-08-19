package net.sparktank.morrigan.gui.editors.mmdb;

import net.sparktank.morrigan.gui.actions.AddToPlaylistAction;
import net.sparktank.morrigan.gui.adaptors.CountsLblProv;
import net.sparktank.morrigan.gui.adaptors.DateAddedLblProv;
import net.sparktank.morrigan.gui.adaptors.DateLastModifiedLblProv;
import net.sparktank.morrigan.gui.adaptors.DateLastPlayerLblProv;
import net.sparktank.morrigan.gui.adaptors.DimensionsLblProv;
import net.sparktank.morrigan.gui.adaptors.DurationLblProv;
import net.sparktank.morrigan.gui.adaptors.FileLblProv;
import net.sparktank.morrigan.gui.adaptors.HashcodeLblProv;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.MediaColumn;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.gui.handler.AddToQueue;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaList;

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

public abstract class MixedMediaListEditor<T extends IMixedMediaList<S>, S extends IMixedMediaItem> extends MediaItemListEditor<T,S> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Column definitions.
	
	public final MediaColumn 
		COL_FILE =       new MediaColumn("file",        new ColumnWeightData(100),            new FileLblProv(this.getImageCache()) );
	public final MediaColumn 
		COL_ADDED =      new MediaColumn("added",       new ColumnPixelData(140, true, true), new DateAddedLblProv()           );
	public final MediaColumn 
		COL_HASH =       new MediaColumn("hash",        new ColumnPixelData( 90, true, true), new HashcodeLblProv(),           SWT.CENTER);
	public final MediaColumn 
		COL_MODIFIED =   new MediaColumn("modified",    new ColumnPixelData(140, true, true), new DateLastModifiedLblProv()    );
	
    public final MediaColumn 
    	COL_COUNTS =     new MediaColumn("counts",      new ColumnPixelData( 70, true, true), new CountsLblProv(),             SWT.CENTER);
    public final MediaColumn 
    	COL_LASTPLAYED = new MediaColumn("last played", new ColumnPixelData(140, true, true), new DateLastPlayerLblProv()      );
    public final MediaColumn 
    	COL_DURATION =   new MediaColumn("duration",    new ColumnPixelData( 60, true, true), new DurationLblProv(),           SWT.RIGHT);
    public final MediaColumn 
    	COL_DIMENSIONS = new MediaColumn("dimensions",  new ColumnPixelData(100, true, true), new DimensionsLblProv(),         SWT.CENTER);
	
    public final MediaColumn[] COLS_UNKNOWN = new MediaColumn[] {
    		this.COL_FILE,
    		this.COL_ADDED,
    		this.COL_HASH,
    		this.COL_MODIFIED,
    };
    
    public final MediaColumn[] COLS_TRACKS = new MediaColumn[] {
    		this.COL_FILE,
    		this.COL_COUNTS,
    		this.COL_ADDED,
    		this.COL_MODIFIED,
    		this.COL_LASTPLAYED,
    		this.COL_HASH,
    		this.COL_DURATION
    };
    
    public final MediaColumn[] COLS_PICTURES = new MediaColumn[] {
    		this.COL_FILE,
    		this.COL_ADDED,
    		this.COL_HASH,
    		this.COL_MODIFIED,
    		this.COL_DIMENSIONS
    };
    
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
						menu.add(new AddToPlaylistAction(e));
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
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

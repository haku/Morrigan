package net.sparktank.morrigan.model.library;

import java.util.ArrayList;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;

public class RemoteLibraryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaLibrary createRemoteLib (String libUrl) throws MorriganException {
		new MorriganMsgDlg("TODO: link to " + libUrl).open();
		
		return null;
	}
	
	public static ArrayList<MediaExplorerItem> getAllRemoteLibraries () {
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		
		
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.model.explorer;

public class MediaExplorerItem extends MenuItem {
	
	// TODO refactor LIBRARY to LOCALLIBRARY.
	public enum ItemType {DISPLAY, LIBRARY, PLAYLIST, REMOTELIBRARY, LOCALGALLERY, LOCALMMDB, REMOTEMMDB}
	
	public ItemType type = null;
	
	public MediaExplorerItem(ItemType type) {
		this.type = type;
	}
	
	public MediaExplorerItem(String identifier, String title, ItemType type) {
		this.identifier = identifier;
		this.title = title;
		this.type = type;
	}
	
}

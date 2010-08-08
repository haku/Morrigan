package net.sparktank.morrigan.gui.editors.pictures;

import net.sparktank.morrigan.model.pictures.gallery.LocalGallery;

import org.eclipse.swt.widgets.Composite;

public class LocalGalleryEditor extends AbstractGalleryEditor<LocalGallery> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LocalGalleryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.
	
	@Override
	protected void createControls(Composite parent) {
		super.createControls(parent);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected boolean handleReadError(Exception e) {
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

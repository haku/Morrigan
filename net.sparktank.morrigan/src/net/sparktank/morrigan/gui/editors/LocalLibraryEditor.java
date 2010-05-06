package net.sparktank.morrigan.gui.editors;

import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;

import net.sparktank.morrigan.gui.actions.LibraryUpdateAction;
import net.sparktank.morrigan.model.library.LocalMediaLibrary;


public class LocalLibraryEditor extends AbstractLibraryEditor<LocalMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LocalLibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void populateToolbar(Composite parent) {
		super.populateToolbar(parent);
		
		prefMenuMgr.add(new Separator());
		prefMenuMgr.add(new LibraryUpdateAction(getMediaList()));
		prefMenuMgr.add(showPropertiesAction);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

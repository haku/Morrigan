package net.sparktank.morrigan.gui.editors;

import java.util.List;

import net.sparktank.morrigan.gui.actions.LibraryUpdateAction;
import net.sparktank.morrigan.gui.display.ActionListener;
import net.sparktank.morrigan.gui.views.ViewLibraryProperties;
import net.sparktank.morrigan.model.library.LocalMediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


public class LocalLibraryEditor extends AbstractLibraryEditor<LocalMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LocalLibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.
	
	protected Button btnAdd = null;
	
	@Override
		protected void createControls(Composite parent) {
			super.createControls(parent);
			
			prefMenuMgr.add(new Separator());
			prefMenuMgr.add(new LibraryUpdateAction(getMediaList()));
			prefMenuMgr.add(showPropertiesAction);
		}
	
	@Override
	protected List<Control> populateToolbar(Composite parent) {
		List<Control> ret = super.populateToolbar(parent);
		
		btnAdd = new Button(parent, SWT.PUSH);
		btnAdd.setImage(iconAdd);
		btnAdd.addSelectionListener(new ActionListener(addAction));
		ret.add(ret.size() - 1, btnAdd);
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	@Override
	protected boolean handleReadError(Exception e) {
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	protected IAction addAction = new Action("Add") {
		public void run () {
			ViewLibraryProperties propView = showLibPropView();
			if (propView!=null) {
				propView.showAddDlg(true);
			}
		}
	};
	
	protected IAction showPropertiesAction = new Action("Properties") {
		public void run () {
			showLibPropView();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

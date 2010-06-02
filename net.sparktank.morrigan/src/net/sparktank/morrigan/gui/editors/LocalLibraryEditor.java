package net.sparktank.morrigan.gui.editors;

import java.util.List;

import net.sparktank.morrigan.gui.actions.LibraryUpdateAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.display.ActionListener;
import net.sparktank.morrigan.gui.views.ViewLibraryProperties;
import net.sparktank.morrigan.model.library.LocalMediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IViewPart;


public class LocalLibraryEditor extends AbstractLibraryEditor<LocalMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LocalLibraryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.
	
	private Button btnAddToQueue = null;
	private Button btnAdd = null;
	
	@Override
		protected void createControls(Composite parent) {
			super.createControls(parent);
			
			prefMenuMgr.add(new Separator());
			prefMenuMgr.add(new LibraryUpdateAction(getMediaList()));
			prefMenuMgr.add(showPropertiesAction);
			
			// Context menu.
			MenuManager contextMenuMgr = new MenuManager();
			contextMenuMgr.add(addToQueueAction);
			contextMenuMgr.add(getAddToMenu());
			contextMenuMgr.add(new Separator());
			contextMenuMgr.add(toggleEnabledAction);
			contextMenuMgr.add(removeAction);
			setTableMenu(contextMenuMgr.createContextMenu(parent));
		}
	
	@Override
	protected List<Control> populateToolbar(Composite parent) {
		List<Control> ret = super.populateToolbar(parent);
		
		btnAddToQueue = new Button(parent, SWT.PUSH);
		btnAddToQueue.setImage(getImageCache().readImage("icons/queue-add.gif"));
		btnAddToQueue.addSelectionListener(new ActionListener(addToQueueAction));
		ret.add(ret.size() - 1, btnAddToQueue);
		
		btnAdd = new Button(parent, SWT.PUSH);
		btnAdd.setImage(getImageCache().readImage("icons/plus.gif"));
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
//	Action helpers.
	
	private ViewLibraryProperties showLibPropView () {
		try {
			IViewPart showView = getSite().getPage().showView(ViewLibraryProperties.ID);
			ViewLibraryProperties viewProp = (ViewLibraryProperties) showView;
			/* FIXME
			 * Instead of checking the type, create separate editors
			 * for remote and local libraries? 
			 */
			if (getMediaList() instanceof LocalMediaLibrary) {
				LocalMediaLibrary ml = (LocalMediaLibrary) getMediaList();
				viewProp.setContent(ml);
			}
			return viewProp;
			
		} catch (Exception e) {
			new MorriganMsgDlg(e).open();
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

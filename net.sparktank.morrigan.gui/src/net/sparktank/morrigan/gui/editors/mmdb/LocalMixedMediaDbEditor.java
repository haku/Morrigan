package net.sparktank.morrigan.gui.editors.mmdb;

import java.util.List;

import net.sparktank.morrigan.gui.actions.DbUpdateAction;
import net.sparktank.morrigan.gui.actions.JumpToAction;
import net.sparktank.morrigan.gui.adaptors.ActionListener;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.views.ViewLibraryProperties;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IViewPart;

public class LocalMixedMediaDbEditor 
		extends AbstractMixedMediaDbEditor<ILocalMixedMediaDb> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LocalMixedMediaDbEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.
	
	private Button btnJumpTo = null;
	private Button btnAddToQueue = null;
	private Button btnAdd = null;
	
	@Override
	protected void createControls(Composite parent) {
		super.createControls(parent);
		
		getPrefMenuMgr().add(new Separator());
		getPrefMenuMgr().add(new DbUpdateAction(getMediaList()));
		getPrefMenuMgr().add(this.showPropertiesAction);
	}
	
	@Override
	protected List<Control> populateToolbar(Composite parent) {
		List<Control> ret = super.populateToolbar(parent);
		
		this.btnJumpTo = new Button(parent, SWT.PUSH);
		this.btnJumpTo.setImage(getImageCache().readImage("icons/search.gif"));
		this.btnJumpTo.addSelectionListener(new ActionListener(new JumpToAction(getSite().getWorkbenchWindow(), getMediaList())));
		ret.add(ret.size() - 1, this.btnJumpTo);
		
		this.btnAddToQueue = new Button(parent, SWT.PUSH);
		this.btnAddToQueue.setImage(getImageCache().readImage("icons/queue-add.gif"));
		this.btnAddToQueue.addSelectionListener(new ActionListener(this.addToQueueAction));
		ret.add(ret.size() - 1, this.btnAddToQueue);
		
		this.btnAdd = new Button(parent, SWT.PUSH);
		this.btnAdd.setImage(getImageCache().readImage("icons/plus.gif"));
		this.btnAdd.addSelectionListener(new ActionListener(this.addAction));
		ret.add(ret.size() - 1, this.btnAdd);
		
		return ret;
	}
	
	@Override
	protected void populateContextMenu(List<IContributionItem> menu0, List<IContributionItem> menu1) {
		menu0.add(new ActionContributionItem(this.addToQueueAction));
		menu0.add(new ActionContributionItem(this.showTagsAction));
		
		menu1.add(new ActionContributionItem(this.toggleEnabledAction));
		menu1.add(new ActionContributionItem(this.removeAction));
		
		super.populateContextMenu(menu0, menu1);
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
		@Override
		public void run () {
			ViewLibraryProperties propView = showLibPropView();
			if (propView!=null) {
				propView.showAddDlg(true);
			}
		}
	};
	
	protected IAction showPropertiesAction = new Action("Properties") {
		@Override
		public void run () {
			showLibPropView();
		}
	};
	
	protected IAction showTagsAction = new Action("Tags") {
		@Override
		public void run () {
			try {
				IViewPart showView = getSite().getPage().showView(ViewTagEditor.ID);
				ViewTagEditor viewTagEd = (ViewTagEditor) showView;
				viewTagEd.setInput(getMediaList(), getSelectedItems());
			}
			catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Action helpers.
	
	ViewLibraryProperties showLibPropView () {
		try {
			IViewPart showView = getSite().getPage().showView(ViewLibraryProperties.ID);
			ViewLibraryProperties viewProp = (ViewLibraryProperties) showView;
			viewProp.setContent(getMediaList());
			return viewProp;
		}
		catch (Exception e) {
			new MorriganMsgDlg(e).open();
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

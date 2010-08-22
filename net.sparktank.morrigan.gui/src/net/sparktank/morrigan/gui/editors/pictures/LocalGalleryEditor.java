package net.sparktank.morrigan.gui.editors.pictures;

import java.util.List;

import net.sparktank.morrigan.gui.actions.DbUpdateAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.views.ViewLibraryProperties;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.model.pictures.gallery.LocalGallery;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;

public class LocalGalleryEditor extends AbstractGalleryEditor<LocalGallery> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.LocalGalleryEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.
	
	@Override
	protected void createControls(Composite parent) {
		super.createControls(parent);
		
		getPrefMenuMgr().add(new Separator());
		getPrefMenuMgr().add(new DbUpdateAction(getMediaList()));
		getPrefMenuMgr().add(this.showPropertiesAction);
	}
	
	@Override
	protected void populateContextMenu(List<IContributionItem> menu0, List<IContributionItem> menu1) {
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

package net.sparktank.morrigan.gui.views;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class ShowViewAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String viewId;
	private final String text2;
	private final ImageDescriptor icon;
	
	public ShowViewAction (String viewId, String text, ImageDescriptor icon ) {
		super();
		this.viewId = viewId;
		this.text2 = text;
		this.icon = icon;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return this.text2; }
	
	@Override
	public String getId() { return "show_" + this.viewId; }
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return this.icon;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void runWithEvent(Event event) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(this.viewId);
		} catch (PartInitException e) {
			new MorriganMsgDlg(e).open();
		}

	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

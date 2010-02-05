package net.sparktank.morrigan.actions;

import net.sparktank.morrigan.dialogs.MorriganMsgDlg;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ShowViewAction extends MorriganAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String viewId;
	private final String text2;
	private final ImageDescriptor icon;
	
	public ShowViewAction (String viewId, String text, ImageDescriptor icon ) {
		super();
		this.viewId = viewId;
		text2 = text;
		this.icon = icon;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return text2; }
	
	@Override
	public String getId() { return "show_" + viewId; }
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return icon;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void runWithEvent(Event event) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewId);
		} catch (PartInitException e) {
			new MorriganMsgDlg(e).open();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

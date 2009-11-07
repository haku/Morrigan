package net.sparktank.morrigan.actions;

import net.sparktank.morrigan.dialogs.MorriganErrDlg;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.PlaylistHelper;
import net.sparktank.morrigan.views.ViewMediaExplorer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;


public class NewPlaylistAction implements IAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IWorkbenchWindow window;
	
	public NewPlaylistAction (IWorkbenchWindow window) {
		super();
		this.window = window;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return "New playlist..."; }
	
	@Override
	public String getId() { return "newpl"; }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void runWithEvent(Event event) {
		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(),
				"", "Enter playlist name.", "newPl", null);
		if (dlg.open() == Window.OK) {
			
			// create playlist.
			String plName = dlg.getValue();
			try {
				PlaylistHelper.instance.createPl(plName);
			} catch (MorriganException e) {
				new MorriganErrDlg(e).open();
				return;
			}
			
			// refresh explorer.
			IWorkbenchPage page = window.getActivePage();
			ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
			view.refresh();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean isHandled() {
		return false;
	}
	@Override
	public boolean isEnabled() {
		return true;
	}
	@Override
	public boolean isChecked() {
		return false;
	}
	@Override
	public String getToolTipText() {
		return null;
	}
	@Override
	public int getStyle() {
		return 0;
	}
	@Override
	public IMenuCreator getMenuCreator() {
		return null;
	}
	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return null;
	}
	@Override
	public HelpListener getHelpListener() {
		return null;
	}
	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return null;
	}
	@Override
	public String getDescription() {
		return null;
	}
	@Override
	public String getActionDefinitionId() {
		return null;
	}
	@Override
	public int getAccelerator() {
		return 0;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {}
	
	@Override
	public void run() {}
	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {}
	@Override
	public void setToolTipText(String text) {}
	@Override
	public void setText(String text) {}
	@Override
	public void setMenuCreator(IMenuCreator creator) {}
	@Override
	public void setImageDescriptor(ImageDescriptor newImage) {}
	@Override
	public void setId(String id) {}
	@Override
	public void setHoverImageDescriptor(ImageDescriptor newImage) {}
	@Override
	public void setHelpListener(HelpListener listener) {}
	@Override
	public void setEnabled(boolean enabled) {}
	@Override
	public void setDisabledImageDescriptor(ImageDescriptor newImage) {}
	@Override
	public void setDescription(String text) {}
	@Override
	public void setChecked(boolean checked) {}
	@Override
	public void setActionDefinitionId(String id) {}
	@Override
	public void setAccelerator(int keycode) {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

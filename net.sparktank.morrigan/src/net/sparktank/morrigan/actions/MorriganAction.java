package net.sparktank.morrigan.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.events.HelpListener;

public abstract class MorriganAction implements IAction {
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

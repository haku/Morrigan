package net.sparktank.morrigan.display;


import org.eclipse.jface.action.IAction;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

public class ActionListener implements SelectionListener {
	
	private final IAction action;

	public ActionListener (IAction action) {
		this.action = action;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		e.widget.getDisplay().asyncExec(new Runnable() {
			public void run() {
				action.run();
			}
		});
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {}
}

package net.sparktank.morrigan.display;


import net.sparktank.morrigan.dialogs.MorriganMsgDlg;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

public class ActionListener implements SelectionListener {
	
	private final IAction action;

	public ActionListener (IAction action) {
		if (action == null) throw new IllegalArgumentException("action can't be null.");
		this.action = action;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		e.widget.getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					action.run();
				} catch (Throwable t) {
					new MorriganMsgDlg(t).open();
				}
			}
		});
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {}
}

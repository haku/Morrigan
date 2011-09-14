package com.vaguehope.morrigan.gui.adaptors;



import org.eclipse.jface.action.IAction;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;

public class ActionListener implements SelectionListener {
	
	final IAction action;

	public ActionListener (IAction action) {
		if (action == null) throw new IllegalArgumentException("action can't be null.");
		this.action = action;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		e.widget.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					ActionListener.this.action.run();
				} catch (Throwable t) {
					new MorriganMsgDlg(t).open();
				}
			}
		});
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
}

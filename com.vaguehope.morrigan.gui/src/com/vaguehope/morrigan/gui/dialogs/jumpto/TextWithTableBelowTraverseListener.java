package com.vaguehope.morrigan.gui.dialogs.jumpto;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;

import com.vaguehope.morrigan.gui.dialogs.Dismissable;

class TextWithTableBelowTraverseListener implements TraverseListener {

	private final TableViewer tableViewer;
	private final Dismissable onEscape;

	public TextWithTableBelowTraverseListener (final TableViewer tableViewer, final Dismissable onEscape) {
		this.tableViewer = tableViewer;
		this.onEscape = onEscape;
	}

	@Override
	public void keyTraversed (final TraverseEvent e) {
		switch (e.detail) {
			case SWT.TRAVERSE_ARROW_NEXT:
				if (e.keyCode == SWT.ARROW_DOWN && this.tableViewer.getTable().getItemCount() > 0) {
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					this.tableViewer.getTable().setSelection(0);
					this.tableViewer.setSelection(this.tableViewer.getSelection());
					this.tableViewer.getTable().setFocus();
				}
				break;
			case SWT.TRAVERSE_ESCAPE:
				e.detail = SWT.TRAVERSE_NONE;
				e.doit = false;
				this.onEscape.dismiss();
				break;
			default:
				break;
		}
	}

}
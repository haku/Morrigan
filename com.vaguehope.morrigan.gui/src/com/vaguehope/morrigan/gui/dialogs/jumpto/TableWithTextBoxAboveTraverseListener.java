package com.vaguehope.morrigan.gui.dialogs.jumpto;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Text;

import com.vaguehope.morrigan.gui.dialogs.Dismissable;

class TableWithTextBoxAboveTraverseListener implements TraverseListener {

	private final TableViewer tableViewer;
	private final Text text;
	private final Dismissable onEscape;

	public TableWithTextBoxAboveTraverseListener (final TableViewer tableViewer, final Text text, final Dismissable onEscape) {
		this.tableViewer = tableViewer;
		this.text = text;
		this.onEscape = onEscape;
	}

	@Override
	public void keyTraversed (final TraverseEvent e) {
		switch (e.detail) {
			case SWT.TRAVERSE_ARROW_PREVIOUS:
				if (this.tableViewer.getTable().getSelectionIndex() == 0) {
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					this.text.setFocus();
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
package com.vaguehope.morrigan.gui.adaptors;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Menu;


public class DropMenuListener implements SelectionListener {
	
	private final Button button;
	private final Menu menu;
	private final MenuManager menuMgr;

	public DropMenuListener (Button button, MenuManager menuMgr) {
		this.button = button;
		this.menuMgr = menuMgr;
		this.menu = null;
	}
	
	public DropMenuListener (Button button, Menu menu) {
		this.button = button;
		this.menuMgr = null;
		this.menu = menu;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		Rectangle rect = this.button.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = this.button.getParent().toDisplay(pt);
		
		Menu m = null;
		if (this.menuMgr != null) {
			m = this.menuMgr.createContextMenu(this.button.getParent());
		} else if (this.menu != null) {
			m = this.menu;
		}
		
		if (m != null) {
			m.setLocation(pt);
			m.setVisible(true);
		}
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
	
}

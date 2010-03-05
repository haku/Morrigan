package net.sparktank.morrigan.display;

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
		Rectangle rect = button.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = button.getParent().toDisplay(pt);
		
		Menu m = null;
		if (menuMgr != null) {
			m = menuMgr.createContextMenu(button.getParent());
		} else if (menu != null) {
			m = menu;
		}
		
		if (m != null) {
			m.setLocation(pt);
			m.setVisible(true);
		}
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {}
	
}

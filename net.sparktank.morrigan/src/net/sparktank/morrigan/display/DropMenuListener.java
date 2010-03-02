package net.sparktank.morrigan.display;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Menu;


public class DropMenuListener implements SelectionListener {
	
	private final Button button;
	private final Menu menu;

	public DropMenuListener (Button button, Menu menu) {
		this.button = button;
		this.menu = menu;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		Rectangle rect = button.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = button.getParent().toDisplay(pt);
		menu.setLocation(pt);
		menu.setVisible(true);
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {}
	
}

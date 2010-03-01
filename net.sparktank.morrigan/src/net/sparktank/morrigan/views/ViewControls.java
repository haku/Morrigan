package net.sparktank.morrigan.views;


import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.helpers.OrderHelper.PlaybackOrder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISizeProvider;
import org.eclipse.ui.part.ViewPart;

public class ViewControls extends ViewPart implements ISizeProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewControls";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void createPartControl(Composite parent) {
		makeIcons();
		makeControls(parent);
	}
	
	@Override
	public void dispose() {
		disposeIcons();
		super.dispose();
	}
	
	@Override
	public void setFocus() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private Image iconPlay;
	private Image iconPause;
	private Image iconStop;
	private Image iconPrev;
	private Image iconNext;
	private int preferedHeight = -1;
	
	private Composite parentComposite;
	private Menu orderModeMenu;
	private Button btnOrderMode;
	
	private void makeIcons () {
		iconPlay = Activator.getImageDescriptor("icons/play.gif").createImage();
		iconPause = Activator.getImageDescriptor("icons/pause.gif").createImage();
		iconStop = Activator.getImageDescriptor("icons/stop.gif").createImage();
		iconPrev = Activator.getImageDescriptor("icons/prev.gif").createImage();
		iconNext = Activator.getImageDescriptor("icons/next.gif").createImage();
	}
	
	private void disposeIcons () {
		iconPlay.dispose();
		iconPause.dispose();
		iconStop.dispose();
	}
	
	private void makeControls (Composite parent) {
		parentComposite = parent;
		
		// Off-screen controls.
		
		orderModeMenu = new Menu(parent.getShell(), SWT.POP_UP);
		for (PlaybackOrder o : PlaybackOrder.values()) {
			MenuItem menuItem = new MenuItem(orderModeMenu, SWT.PUSH);
			menuItem.setText(o.toString());
			menuItem.addSelectionListener(menuOrderModeListener);
		}
		
		// On-screen controls.
		
		FormLayout layout = new FormLayout();
		parent.setLayout(layout);
		
		final int sep = 3;
		FormData formData;
		
		Button btnStop = new Button(parent, SWT.PUSH);
		btnStop.setImage(iconStop);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(0, sep);
		btnStop.setLayoutData(formData);
		
		Button btnPlayPause = new Button(parent, SWT.PUSH);
		btnPlayPause.setImage(iconPlay);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(btnStop, sep);
		btnPlayPause.setLayoutData(formData);

		Button btnPrev = new Button(parent, SWT.PUSH);
		btnPrev.setImage(iconPrev);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(btnPlayPause, sep);
		btnPrev.setLayoutData(formData);
		
		Button btnNext = new Button(parent, SWT.PUSH);
		btnNext.setImage(iconNext);
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.left = new FormAttachment(btnPrev, sep);
		btnNext.setLayoutData(formData);
		
		Label label = new Label(parent, SWT.NONE);
		label.setText("Playing 0:15 of 12:34");
		formData = new FormData();
		formData.top = new FormAttachment(50, -(label.computeSize(SWT.DEFAULT, SWT.DEFAULT).y)/2);
		formData.left = new FormAttachment(btnNext, sep*2);
		label.setLayoutData(formData);
		
		btnOrderMode = new Button(parent, SWT.PUSH);
		btnOrderMode.setText(PlaybackOrder.SEQUENTIAL.toString());
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(100, -sep);
		btnOrderMode.setLayoutData(formData);
		btnOrderMode.addSelectionListener(btnOrderModeListener);
		
		preferedHeight = sep + btnStop.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + sep;
	}
	
	@Override
	public int computePreferredSize(boolean width, int availableParallel, int availablePerpendicular, int preferredResult) {
		if (preferedHeight > 0 ) {
			return preferedHeight;
		} else {
			return 30;
		}
	}
	
	@Override
	public int getSizeFlags(boolean width) {
		return SWT.MAX;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Control events.
	
	private SelectionAdapter menuOrderModeListener = new SelectionAdapter () {
		@Override
		public void widgetSelected(SelectionEvent e) {
			btnOrderMode.setText(((MenuItem) e.widget).getText());
		}
	};
	
	private SelectionListener btnOrderModeListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Rectangle rect = ((Button)e.widget).getBounds();
			Point pt = new Point(rect.x, rect.y + rect.height);
			pt = parentComposite.toDisplay(pt);
			orderModeMenu.setLocation(pt);
			orderModeMenu.setVisible(true);
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}
	};
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

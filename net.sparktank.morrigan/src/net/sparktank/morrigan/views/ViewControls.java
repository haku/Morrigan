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
	
	private void makeControls (final Composite parent) {
//		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
//		layout.center = true;
//		layout.spacing = layout.spacing * 2;
//		parent.setLayout(layout);
		
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
		
		final Button btnOrderMode = new Button(parent, SWT.PUSH);
		btnOrderMode.setText("Sequencial");
		formData = new FormData();
		formData.top = new FormAttachment(0, sep);
		formData.right = new FormAttachment(100, -sep);
		btnOrderMode.setLayoutData(formData);
		
		btnOrderMode.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Menu menu = new Menu(parent.getShell(), SWT.POP_UP);
				for (PlaybackOrder o : PlaybackOrder.values()) {
					MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
					menuItem.setText(o.toString());
					menuItem.addSelectionListener(new SelectionAdapter () {
						@Override
						public void widgetSelected(SelectionEvent e) {
							btnOrderMode.setText(((MenuItem) e.widget).getText());
						}
					});
				}
				Rectangle rect = btnOrderMode.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = parent.toDisplay(pt);
				menu.setLocation(pt);
				menu.setVisible(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
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
}

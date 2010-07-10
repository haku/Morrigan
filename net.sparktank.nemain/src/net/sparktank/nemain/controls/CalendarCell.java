package net.sparktank.nemain.controls;

import net.sparktank.nemain.model.NemainDate;
import net.sparktank.nemain.model.NemainEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class CalendarCell {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public interface ICalendarCellEditEvent {
		public void editBtnClicked (NemainDate date, NemainEvent event, boolean anual);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private NemainDate date;
	private NemainEvent anualEvent;
	private NemainEvent event;
	private ICalendarCellEditEvent cellEditEventListener;
	
	Composite composite;
	private Label label;
	private Button button;
	
	public CalendarCell (Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		label = new Label(composite, SWT.PUSH + SWT.WRAP + SWT.BORDER);
		button = new Button(composite, SWT.PUSH);
		
		FormData formData;
		composite.setLayout(new FormLayout());
		
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		button.setLayoutData(formData);
		
		formData = new FormData();
		formData.top = new FormAttachment(button, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		label.setLayoutData(formData);
		
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (cellEditEventListener != null) {
					try {
						if ((e.stateMask & SWT.SHIFT) != 0 || (e.stateMask & SWT.CONTROL) != 0) {
    						cellEditEventListener.editBtnClicked(date, anualEvent, true);
						} else {
    						cellEditEventListener.editBtnClicked(date, event, false);
    					}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
	
	public Composite getComposite () {
		return composite;
	}
	
	private void update () {
		button.setText(date.toString());
		
		StringBuilder sb = new StringBuilder();
		
		if (anualEvent != null) {
			sb.append(anualEvent.getEntryText());
		}
		
		if (event != null) {
			if (anualEvent != null) {
				sb.append("\n\n");
			}
			sb.append(event.getEntryText());
		}
		
		label.setText(sb.toString());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setDate (NemainDate date) {
		this.date = date;
		this.anualEvent = null;
		this.event = null;
		update();
	}
	
	public void setAnualEvent (NemainEvent anualEvent) {
		this.anualEvent = anualEvent;
		update();
	}
	
	public void setEvent (NemainEvent event) {
		this.event = event;
		update();
	}
	
	public void setCellEditEventListener (ICalendarCellEditEvent listener) {
		this.cellEditEventListener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

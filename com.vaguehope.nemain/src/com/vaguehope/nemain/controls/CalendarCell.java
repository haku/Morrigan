/*
 * Copyright 2010 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.nemain.controls;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaguehope.nemain.model.NemainDate;
import com.vaguehope.nemain.model.NemainEvent;

public class CalendarCell {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	NemainDate date;
	NemainEvent anualEvent;
	NemainEvent event;
	CalendarCellEditEventHandler cellEditEventListener;

	Composite composite;
	private Label label;
	private Button button;

	public CalendarCell (Composite parent) {
		this.composite = new Composite(parent, SWT.NONE);
		this.label = new Label(this.composite, SWT.PUSH + SWT.WRAP + SWT.BORDER);
		this.button = new Button(this.composite, SWT.PUSH);

		FormData formData;
		this.composite.setLayout(new FormLayout());

		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		this.button.setLayoutData(formData);

		formData = new FormData();
		formData.top = new FormAttachment(this.button, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		this.label.setLayoutData(formData);

		this.button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (CalendarCell.this.cellEditEventListener != null) {
					if ((e.stateMask & SWT.SHIFT) != 0 || (e.stateMask & SWT.CONTROL) != 0) {
						CalendarCell.this.cellEditEventListener.editBtnClicked(CalendarCell.this.date, CalendarCell.this.anualEvent, true);
					}
					else {
						CalendarCell.this.cellEditEventListener.editBtnClicked(CalendarCell.this.date, CalendarCell.this.event, false);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { /* UNUSED */ }
		});
	}

	public Composite getComposite () {
		return this.composite;
	}

	private void update () {
		this.button.setText(this.date.toString());

		StringBuilder sb = new StringBuilder();

		if (this.anualEvent != null) {
			sb.append(this.anualEvent.getEntryText());
		}

		if (this.event != null) {
			if (this.anualEvent != null) {
				sb.append("\n\n");
			}
			sb.append(this.event.getEntryText());
		}

		this.label.setText(sb.toString());
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

	public void setCellEditEventListener (CalendarCellEditEventHandler listener) {
		this.cellEditEventListener = listener;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

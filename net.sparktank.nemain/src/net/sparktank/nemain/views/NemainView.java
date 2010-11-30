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

package net.sparktank.nemain.views;


import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.nemain.config.Config;
import net.sparktank.nemain.controls.CalendarCell;
import net.sparktank.nemain.controls.CalendarCell.ICalendarCellEditEvent;
import net.sparktank.nemain.helpers.ImageCache;
import net.sparktank.nemain.model.NemainDate;
import net.sparktank.nemain.model.NemainEvent;
import net.sparktank.nemain.model.SqliteLayer;
import net.sparktank.nemain.shells.EditEntryShell;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class NemainView extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.nemain.views.NemainView";
	
	public static final int GRID_ROW_LENGTH = 7;
	public static final int GRID_ROW_COUNT = 3;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		try {
			initDataSource();
		} catch (DbException e) {
			throw new RuntimeException(e);
		}
		createControls(parent);
		
		Calendar cal = Calendar.getInstance();
		setCurrentDate(new NemainDate().daysAfter(-cal.get(Calendar.DAY_OF_WEEK)+2)); // Start on a Monday.
	}
	
	@Override
	public void dispose() {
		try {
			closeDataSource();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data providers.
	
	private static final String FILE_CAL_DB = "nemain.db3";
	SqliteLayer _dataSource;
	private NemainDate _currentDate = new NemainDate();
	
	private void initDataSource () throws DbException {
		String fullPathToDb = Config.getFullPathToDb(FILE_CAL_DB);
		this._dataSource = new SqliteLayer(fullPathToDb);
		System.err.println("Connected to '"+fullPathToDb+"'.");
	}
	
	private void closeDataSource () {
		this._dataSource.dispose();
		System.err.println("Disconnected '"+this._dataSource.getDbFilePath()+"'.");
	}
	
	void setCurrentDate (NemainDate date) {
		this._currentDate = date;
		this.lblStatus.setText(date.toString());
		
		// TODO do query in DB layer?
		List<NemainEvent> data;
		try {
			data = this._dataSource.getEvents();
		} catch (DbException e) {
			throw new RuntimeException(e);
		}
		List<NemainEvent> eventsToShow = new LinkedList<NemainEvent>();
		for (NemainEvent event : data) {
			if (event.isWithinNDaysAfter(getCurrentDate(), this.gridButton.length)) {
				eventsToShow.add(event);
			}
		}
		
		for (int i = 0; i < this.gridButton.length; i++) {
			NemainDate d = date.daysAfter(i);
			this.gridButton[i].setDate(d);
			
			for (NemainEvent event : eventsToShow) {
				if (event.isSameDay(d)) {
					if (event.getYear() == 0) {
						this.gridButton[i].setAnualEvent(event);
					} else {
						this.gridButton[i].setEvent(event);
					}
				}
			}
		}
		
		this.viewParent.layout();
	}
	
	NemainDate getCurrentDate () {
		return this._currentDate;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI controls.
	
	private ImageCache imageCache = new ImageCache();
	protected final int sep = 3;
	
	private Composite viewParent;
	
	private Button btnDateBack;
	private Button btnDateForward;
	private Label lblStatus;
	
	Composite gridContainer;
	private CalendarCell[] gridButton;
	
	private void createControls (Composite parent) {
		this.viewParent = parent;
		FormData formData;
		parent.setLayout(new FormLayout());
		
		Composite tbCom = new Composite(parent, SWT.NONE);
		this.btnDateBack = new Button(tbCom, SWT.PUSH);
		this.btnDateForward = new Button(tbCom, SWT.PUSH);
		this.lblStatus = new Label(tbCom, SWT.NONE);
		
		this.gridContainer = new Composite(parent, SWT.NONE);
		this.gridButton = new CalendarCell[GRID_ROW_LENGTH * GRID_ROW_COUNT];
		for (int i = 0; i < this.gridButton.length; i++) {
			this.gridButton[i] = new CalendarCell(this.gridContainer);
		}
		
		tbCom.setLayout(new FormLayout());
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		tbCom.setLayoutData(formData);
		
		this.btnDateBack.setImage(this.imageCache.readImage("icons/minus.gif"));
		formData = new FormData();
		formData.left = new FormAttachment(0, this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnDateBack.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(this.btnDateBack, this.sep);
		formData.right = new FormAttachment(this.btnDateForward, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.lblStatus.setLayoutData(formData);
		this.lblStatus.setAlignment(SWT.CENTER);
		
		this.btnDateForward.setImage(this.imageCache.readImage("icons/plus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(100, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnDateForward.setLayoutData(formData);
		
		GridLayout gridLayout = new GridLayout(GRID_ROW_LENGTH, true);
		this.gridContainer.setLayout(gridLayout);
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.gridContainer.setLayoutData(formData);
		
		for (int i = 0; i < this.gridButton.length; i++) {
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			this.gridButton[i].getComposite().setLayoutData(gridData);
		}
		
		this.btnDateBack.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setCurrentDate(getCurrentDate().daysAfter(-7));
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
		});
		
		this.btnDateForward.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setCurrentDate(getCurrentDate().daysAfter(7));
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
		});
		
		ICalendarCellEditEvent listener = new ICalendarCellEditEvent() {
			@Override
			public void editBtnClicked(NemainDate date, NemainEvent event, boolean anual) {
				NemainEvent eventClicked;
				
				if (event == null) {
					if (anual) {
						eventClicked = new NemainEvent("", 0, date.getMonth(), date.getDay());
					} else {
						eventClicked = new NemainEvent("", date.getYear(), date.getMonth(), date.getDay());
					}
				}
				else {
					eventClicked = event;
				}
				
				EditEntryShell editEntryShell = new EditEntryShell(getSite().getShell());
				if (editEntryShell.showDlg(eventClicked)) {
					NemainEvent newEvent = new NemainEvent(editEntryShell.getExitText(), eventClicked);
					try {
						NemainView.this._dataSource.setEvent(newEvent);
					} catch (DbException e) {
						e.printStackTrace();
					}
					setCurrentDate(getCurrentDate());
				}
			}
		};
		for (int i = 0; i < this.gridButton.length; i++) {
			this.gridButton[i].setCellEditEventListener(listener);
		}
		
	}
	
	@Override
	public void setFocus() {
//		viewer.getControl().setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
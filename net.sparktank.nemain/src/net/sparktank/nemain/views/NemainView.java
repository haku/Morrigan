/*
 * Copyright 2010, 2011 Fae Hutter
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
import net.sparktank.nemain.controls.CalendarCellEditEventHandler;
import net.sparktank.nemain.controls.CalendarPlot;
import net.sparktank.nemain.controls.CalendarPlotDataSource;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class NemainView extends ViewPart implements CalendarPlotDataSource {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.nemain.views.NemainView";
	
	public static final int GRID_ROW_COUNT = 3; // Default row count.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart life-cycle methods.
	
	@Override
	public void createPartControl(Composite parent) {
		try {
			initDataSource();
		} catch (DbException e) {
			throw new RuntimeException(e);
		}
		createControls(parent);
		
		Calendar cal = Calendar.getInstance();
		setFirstCellDate(new NemainDate().daysAfter(-cal.get(Calendar.DAY_OF_WEEK)+2)); // Start on a Monday.
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
	
	@Override
	public void setFocus() {
		this.calendarPlot.setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart state methods.
	
	private static final String KEY_ROWCOUNT = "ROWCOUNT";
	private int savedRowCount = GRID_ROW_COUNT;
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		
		memento.putInteger(KEY_ROWCOUNT, this.calendarPlot.getRowCount());
	}
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		
		Integer i = memento.getInteger(KEY_ROWCOUNT);
		if (i != null) this.savedRowCount = i.intValue();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data providers.
	
	private static final String FILE_CAL_DB = "nemain.db3";
	protected SqliteLayer _dataSource;
	
	private void initDataSource () throws DbException {
		String fullPathToDb = Config.getFullPathToDb(FILE_CAL_DB);
		this._dataSource = new SqliteLayer(fullPathToDb);
		System.err.println("Connected to '"+fullPathToDb+"'.");
	}
	
	private void closeDataSource () {
		this._dataSource.dispose();
		System.err.println("Disconnected '"+this._dataSource.getDbFilePath()+"'.");
	}
	
	protected void setFirstCellDate (NemainDate date) {
		this.lblStatus.setText(date.toString());
		this.calendarPlot.setFirstCellDate(date);
	}
	
	protected NemainDate getFirstCellDate () {
		return this.calendarPlot.getFirstCellDate();
	}
	
	@Override
	public List<NemainEvent> getCalendarEvents(NemainDate firstDate, int dayCount) {
		/*
		 * FIXME TODO do query in DB layer!
		 */
		
		List<NemainEvent> data;
		try {
			data = this._dataSource.getEvents();
		} catch (DbException e) {
			throw new RuntimeException(e);
		}
		List<NemainEvent> eventsToShow = new LinkedList<NemainEvent>();
		for (NemainEvent event : data) {
			if (event.isWithinNDaysAfter(firstDate, dayCount)) {
				eventsToShow.add(event);
			}
		}
		
		return eventsToShow;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI controls.
	
	private ImageCache imageCache = new ImageCache();
	protected final int sep = 3;
	
	private Button btnDateBack;
	private Button btnDateForward;
	private Label lblStatus;
	private Button btnRowsMore;
	private Button btnRowsLess;
	
	CalendarPlot calendarPlot;
	
	private void createControls (Composite parent) {
		FormData formData;
		parent.setLayout(new FormLayout());
		
		Composite tbCom = new Composite(parent, SWT.NONE);
		this.btnDateBack = new Button(tbCom, SWT.PUSH);
		this.btnDateForward = new Button(tbCom, SWT.PUSH);
		this.lblStatus = new Label(tbCom, SWT.NONE);
		this.btnRowsMore = new Button(tbCom, SWT.PUSH);
		this.btnRowsLess = new Button(tbCom, SWT.PUSH);
		this.calendarPlot = new CalendarPlot(parent, this.savedRowCount);
		
		parent.setTabList(new Control[] {this.calendarPlot, tbCom} );
		
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
		
		this.btnDateForward.setImage(this.imageCache.readImage("icons/plus.gif"));
		formData = new FormData();
		formData.left = new FormAttachment(this.btnDateBack, this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnDateForward.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(this.btnDateForward, this.sep);
		formData.right = new FormAttachment(this.btnRowsMore, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.lblStatus.setLayoutData(formData);
		this.lblStatus.setAlignment(SWT.CENTER);
		
		this.btnRowsMore.setImage(this.imageCache.readImage("icons/plus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(this.btnRowsLess, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnRowsMore.setLayoutData(formData);
		
		this.btnRowsLess.setImage(this.imageCache.readImage("icons/minus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(100, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnRowsLess.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, 0);
		this.calendarPlot.setLayoutData(formData);
		
		this.btnDateBack.addSelectionListener(this.nextListener);
		this.btnDateForward.addSelectionListener(this.prevListener);
		this.btnRowsMore.addSelectionListener(this.moreRowsListener);
		this.btnRowsLess.addSelectionListener(this.lessRowsListener);
		this.calendarPlot.setCellEditEventListener(this.listener);
		
		this.calendarPlot.setDataSource(this);
	}
	
	private SelectionListener nextListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			setFirstCellDate(getFirstCellDate().daysAfter(-7));
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
	};
	
	private SelectionListener prevListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			setFirstCellDate(getFirstCellDate().daysAfter(7));
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
	};
	
	private SelectionListener moreRowsListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			NemainView.this.calendarPlot.setRowCount(NemainView.this.calendarPlot.getRowCount() + 1);
			setFirstCellDate(getFirstCellDate()); // FIXME TODO replace this hack by giving calendarPlot at data source object.
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
	};
	
	private SelectionListener lessRowsListener = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			NemainView.this.calendarPlot.setRowCount(NemainView.this.calendarPlot.getRowCount() > 1 ? NemainView.this.calendarPlot.getRowCount() - 1 : 1);
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
	};
	
	private CalendarCellEditEventHandler listener = new CalendarCellEditEventHandler() {
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
				NemainView.this.calendarPlot.dataChanged();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
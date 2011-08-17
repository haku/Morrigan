/*
 * Copyright 2010, 2011 Alex Hutter
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

package com.vaguehope.nemain.views;


import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.vaguehope.nemain.config.Config;
import com.vaguehope.nemain.controls.CalendarCellEditEventHandler;
import com.vaguehope.nemain.controls.CalendarPlot;
import com.vaguehope.nemain.controls.CalendarPlotDataSource;
import com.vaguehope.nemain.model.NemainDate;
import com.vaguehope.nemain.model.NemainEvent;
import com.vaguehope.nemain.model.SqliteLayer;
import com.vaguehope.nemain.shells.EditEntryShell;

public class NemainView extends ViewPart implements CalendarPlotDataSource {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "com.vaguehope.nemain.views.NemainView";
	
	public static final int GRID_ROW_COUNT = 3; // Default row count.
	public static final int GRID_ROW_COUNT_MAX = 10;
	
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
		cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
		this.calendarPlot.setFirstCellDate(new NemainDate(cal));
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
		
		if (memento != null) {
    		Integer i = memento.getInteger(KEY_ROWCOUNT);
    		if (i != null) this.savedRowCount = i.intValue();
		}
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
	
	protected final int sep = 3;
	
	CalendarPlot calendarPlot;
	
	private void createControls (Composite parent) {
		FormData formData;
		parent.setLayout(new FormLayout());
		
		for (int i = 1; i <= GRID_ROW_COUNT_MAX; i++) {
			final int n = i;
    		getViewSite().getActionBars().getMenuManager().add(new Action (n + " rows") {
    			@Override
    			public void run() {
    				NemainView.this.calendarPlot.setRowCount(n);
    			}
    		});
		}
		
		this.calendarPlot = new CalendarPlot(parent, this.savedRowCount);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(100, 0);
		this.calendarPlot.setLayoutData(formData);
		
		this.calendarPlot.setCellEditEventListener(this.listener);
		this.calendarPlot.setDataSource(this);
	}
	
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
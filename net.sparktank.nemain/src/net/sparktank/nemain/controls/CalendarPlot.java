/*
 * Copyright 2011 Fae Hutter
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

package net.sparktank.nemain.controls;

import java.util.HashMap;
import java.util.Map;

import net.sparktank.nemain.model.NemainDate;
import net.sparktank.nemain.model.NemainEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class CalendarPlot extends Canvas implements PaintListener, MouseListener, MouseWheelListener, KeyListener, TraverseListener, FocusListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final int DAYS_IN_WEEK = 7;
	public static final int CELL_PADDING = 5; // px.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int rows;
	private NemainDate firstCellDate;
	private CalendarCellEditEventHandler cellEditEventListener;
	private CalendarPlotDataSource dataSource;
	
	private Map<NemainDate, NemainEvent> singleEvents;
	private Map<NemainDate, NemainEvent> anualEvents;
	
	private int cellW;
	private int cellH;
	private int selectedCell;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public CalendarPlot (Composite parent, int rows) {
		super(parent, SWT.NONE);
		
		addPaintListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		addTraverseListener(this);
		addFocusListener(this);
		
		this.rows = rows;
		this.selectedCell = 0;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data methods.
	
	public void setRowCount (int rows) {
		if (rows > this.rows) fetchData(); // If there are going to be more rows of data, fetch new data.
		this.rows = rows;
		redraw();
	}
	
	public int getRowCount () {
		return this.rows;
	}
	
	public int dayCount () {
		return DAYS_IN_WEEK * this.rows;
	}
	
	public void setFirstCellDate (NemainDate firstCellDate) {
		this.firstCellDate = firstCellDate;
		fetchData();
		redraw();
	}
	
	public void setDataSource (CalendarPlotDataSource dataSource) {
		this.dataSource = dataSource;
		fetchData();
		redraw();
	}
	
	public void setCellEditEventListener (CalendarCellEditEventHandler listener) {
		this.cellEditEventListener = listener;
	}
	
	public void setSelectedCell (int selected) {
		if (this.selectedCell == selected) return;
		
		final int MAX = (this.rows * DAYS_IN_WEEK) - 1;
		if (selected > MAX) {
			NemainDate newFirstCellDate = this.firstCellDate.daysAfter(DAYS_IN_WEEK);
			this.selectedCell = selected - DAYS_IN_WEEK;
			setFirstCellDate(newFirstCellDate);
			redraw();
		}
		else if (selected < 0) {
			NemainDate newFirstCellDate = this.firstCellDate.daysAfter(-DAYS_IN_WEEK);
			this.selectedCell = selected + DAYS_IN_WEEK;
			setFirstCellDate(newFirstCellDate);
			redraw();
		}
		else {
			this.selectedCell = selected;
			redraw();
		}
	}
	
	private void fetchData () {
		this.singleEvents = new HashMap<NemainDate, NemainEvent>();
		this.anualEvents = new HashMap<NemainDate, NemainEvent>();
		
		if (this.dataSource != null && this.firstCellDate != null) {
    		for (NemainEvent event : this.dataSource.getCalendarEvents(this.firstCellDate, dayCount())) {
    			if (event.getYear() == 0) {
    				this.anualEvents.put(new NemainDate(event), event);
    			}
    			else {
    				this.singleEvents.put(new NemainDate(event), event);
    			}
    		}
		}
	}
	
	private void clickCell(int i, boolean anual) {
		NemainDate date = this.firstCellDate.daysAfter(i);
		
		NemainEvent event;
		if (anual) {
			event = this.anualEvents.get(new NemainDate(0, date.getMonth(), date.getDay())); // FIXME this is ugly.
		}
		else {
			event = this.singleEvents.get(date);
		}
		
		this.cellEditEventListener.editBtnClicked(date, event, anual);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	PaintListener methods.
	
	@Override
	public void paintControl(PaintEvent e) {
		Rectangle clientArea = getClientArea();
		this.cellW = clientArea.width / DAYS_IN_WEEK;
		this.cellH = clientArea.height / this.rows;
		
		final TextLayout msgLayout = new TextLayout(getDisplay());
		try {
			msgLayout.setWidth(this.cellW - (CELL_PADDING * 2));
			
			for (int i = 0; i < dayCount(); i++) {
				Rectangle cell = new Rectangle((i % 7) * this.cellW, (int) Math.floor(i / 7) * this.cellH, this.cellW, this.cellH);
				
				// Is selected cell?  If so draw it as such.
				if (i == this.selectedCell) {
					e.gc.fillRectangle(cell);
					if (isFocusControl()) {
						e.gc.drawFocus(cell.x + CELL_PADDING, cell.y + CELL_PADDING, cell.width - CELL_PADDING * 2, cell.height - CELL_PADDING * 2);
					}
				}
				
				// Draw border.
				e.gc.drawRectangle(cell);
				
				// Draw content.
				if (this.firstCellDate != null) {
					// Draw date title.
					NemainDate date = this.firstCellDate.daysAfter(i);
					Rectangle titleRect = drawTextHCen(e, cell.x + (cell.width / 2), cell.y, date.getDateAsString());
					
					// Draw message(s).
					int msgTop = titleRect.y + titleRect.height;
					NemainEvent anualEvent = this.anualEvents.get(new NemainDate(0, date.getMonth(), date.getDay())); // FIXME this is ugly.
					if (anualEvent != null) {
						msgLayout.setText(anualEvent.getEntryText());
						msgLayout.draw(e.gc, cell.x + CELL_PADDING, msgTop);
						msgTop = msgTop + msgLayout.getBounds().height;
					}
					NemainEvent singleEvent = this.singleEvents.get(date);
					if (singleEvent != null) {
						msgLayout.setText(singleEvent.getEntryText());
						msgLayout.draw(e.gc, cell.x + CELL_PADDING, msgTop);
					}
				}
			}
		}
		finally {
			msgLayout.dispose();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Keyboard events.
	
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.keyCode == SWT.ARROW_RIGHT) {
			setSelectedCell(this.selectedCell + 1);
		}
		else if (e.keyCode == SWT.ARROW_LEFT) {
			setSelectedCell(this.selectedCell - 1);
		}
		else if (e.keyCode == SWT.ARROW_DOWN) {
			setSelectedCell(this.selectedCell + DAYS_IN_WEEK);
		}
		else if (e.keyCode == SWT.ARROW_UP) {
			setSelectedCell(this.selectedCell - DAYS_IN_WEEK);
		}
		else if (e.keyCode == 13) { // TODO find constant value.
			clickCell(this.selectedCell, (e.stateMask & SWT.SHIFT) != 0 || (e.stateMask & SWT.CONTROL) != 0);
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) { /* UNUSED */ }
	
	@Override
	public void keyTraversed(TraverseEvent e) {
		e.doit = true;
	}
	
	@Override
	public void focusGained(FocusEvent e) {
		redraw();
	}
	
	@Override
	public void focusLost(FocusEvent e) {
		redraw();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MouseListener methods.
	
	@Override
	public void mouseDoubleClick(MouseEvent e) {
		int c = (int) Math.floor(e.x / this.cellW);
		int r = (int) Math.floor(e.y / this.cellH);
		int i = (r * DAYS_IN_WEEK) + c;
		clickCell(i, ((e.stateMask & SWT.SHIFT) != 0 || (e.stateMask & SWT.CONTROL) != 0));
	}
	
	@Override
	public void mouseDown(MouseEvent e) {
		int c = (int) Math.floor(e.x / this.cellW);
		int r = (int) Math.floor(e.y / this.cellH);
		int i = (r * DAYS_IN_WEEK) + c;
		
		setSelectedCell(i);
	}
	
	@Override
	public void mouseUp(MouseEvent e) { /* UNUSED */ }
	
	@Override
	public void mouseScrolled(MouseEvent e) {
		System.err.println("mouse wheel " + e.count);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Rectangle drawTextHCen (PaintEvent e, int x, int top, String text) {
		Point textSize = e.gc.textExtent(text);
		int _left = x - (textSize.x / 2);
		e.gc.drawText(text, _left, top, SWT.DRAW_TRANSPARENT);
		return new Rectangle(_left, top, textSize.x, textSize.y);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.nemain.views;


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
		setCurrentDate(new NemainDate());
	}
	
	@Override
	public void dispose() {
		try {
			closeDataSource();
		} catch (DbException e) {
			e.printStackTrace();
		}
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data providers.
	
	private static final String FILE_CAL_DB = "nemain.db3";
	private SqliteLayer _dataSource;
	private NemainDate _currentDate = new NemainDate();
	
	private void initDataSource () throws DbException {
		String fullPathToDb = Config.getFullPathToDb(FILE_CAL_DB);
		_dataSource = new SqliteLayer(fullPathToDb);
		System.err.println("Connected to '"+fullPathToDb+"'.");
	}
	
	private void closeDataSource () throws DbException {
		_dataSource.dispose();
		System.err.println("Disconnected '"+_dataSource.getDbFilePath()+"'.");
	}
	
	private void setCurrentDate (NemainDate date) {
		_currentDate = date;
		lblStatus.setText(date.toString());
		
		// TODO do query in DB layer?
		List<NemainEvent> data;
		try {
			data = _dataSource.getEvents();
		} catch (DbException e) {
			throw new RuntimeException(e);
		}
		List<NemainEvent> eventsToShow = new LinkedList<NemainEvent>();
		for (NemainEvent event : data) {
			if (event.isWithinNDaysAfter(getCurrentDate(), gridButton.length)) {
				eventsToShow.add(event);
			}
		}
		
		for (int i = 0; i < gridButton.length; i++) {
			NemainDate d = date.daysAfter(i);
			gridButton[i].setDate(d);
			
			for (NemainEvent event : eventsToShow) {
				if (event.isSameDay(d)) {
					if (event.getYear() == 0) {
						gridButton[i].setAnualEvent(event);
					} else {
						gridButton[i].setEvent(event);
					}
				}
			}
		}
		
		parent.layout();
	}
	
	private NemainDate getCurrentDate () {
		return _currentDate;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI controls.
	
	private ImageCache imageCache = new ImageCache();
	protected final int sep = 3;
	
	private Composite parent;
	
	private Button btnDateBack;
	private Button btnDateForward;
	private Label lblStatus;
	
	Composite gridContainer;
	private CalendarCell[] gridButton;
	
	private void createControls (Composite parent) {
		this.parent = parent;
		FormData formData;
		parent.setLayout(new FormLayout());
		
		Composite tbCom = new Composite(parent, SWT.NONE);
		btnDateBack = new Button(tbCom, SWT.PUSH);
		btnDateForward = new Button(tbCom, SWT.PUSH);
		lblStatus = new Label(tbCom, SWT.NONE);
		
		gridContainer = new Composite(parent, SWT.NONE);
		gridButton = new CalendarCell[GRID_ROW_LENGTH * GRID_ROW_COUNT];
		for (int i = 0; i < gridButton.length; i++) {
			gridButton[i] = new CalendarCell(gridContainer);
		}
		
		tbCom.setLayout(new FormLayout());
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		tbCom.setLayoutData(formData);
		
		btnDateBack.setImage(imageCache.readImage("icons/minus.gif"));
		formData = new FormData();
		formData.left = new FormAttachment(0, sep);
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		btnDateBack.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(btnDateBack, sep);
		formData.right = new FormAttachment(btnDateForward, -sep);
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		lblStatus.setLayoutData(formData);
		lblStatus.setAlignment(SWT.CENTER);
		
		btnDateForward.setImage(imageCache.readImage("icons/plus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(100, -sep);
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		btnDateForward.setLayoutData(formData);
		
		GridLayout gridLayout = new GridLayout(GRID_ROW_LENGTH, true);
		gridContainer.setLayout(gridLayout);
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, -sep);
		gridContainer.setLayoutData(formData);
		
		for (int i = 0; i < gridButton.length; i++) {
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridButton[i].getComposite().setLayoutData(gridData);
		}
		
		btnDateBack.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setCurrentDate(getCurrentDate().daysAfter(-1));
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		btnDateForward.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setCurrentDate(getCurrentDate().daysAfter(1));
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		ICalendarCellEditEvent listener = new ICalendarCellEditEvent() {
			@Override
			public void editBtnClicked(NemainDate date, NemainEvent event, boolean anual) {
				if (event == null) {
					if (anual) {
						event = new NemainEvent("", 0, date.getMonth(), date.getDay());
					} else {
						event = new NemainEvent("", date.getYear(), date.getMonth(), date.getDay());
					}
				}
				
				EditEntryShell editEntryShell = new EditEntryShell(getSite().getShell());
				if (editEntryShell.showDlg(event)) {
					String newText = editEntryShell.getExitText();
					System.err.println("TODO: save new text: " + newText);
				}
				
			}
		};
		for (int i = 0; i < gridButton.length; i++) {
			gridButton[i].setCellEditEventListener(listener);
		}
		
	}
	
	public void setFocus() {
//		viewer.getControl().setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
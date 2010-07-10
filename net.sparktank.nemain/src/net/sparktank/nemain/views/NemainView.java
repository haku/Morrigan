package net.sparktank.nemain.views;


import java.util.LinkedList;
import java.util.List;

import net.sparktank.nemain.config.Config;
import net.sparktank.nemain.helpers.ImageCache;
import net.sparktank.nemain.model.NemainDate;
import net.sparktank.nemain.model.NemainEvent;
import net.sparktank.nemain.model.SqliteLayer;
import net.sparktank.nemain.shells.EditEntryShell;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class NemainView extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.nemain.views.NemainView";
	
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
		viewer.refresh();
	}
	
	private NemainDate getCurrentDate () {
		return _currentDate;
	}
	
	private class ViewContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object parent) {
			List<NemainEvent> data;
			try {
				data = _dataSource.getEvents();
			}
			catch (DbException e) {
				return new String[] {};
			}
			
			// TODO do this is DB query?
			List<NemainEvent> ret = new LinkedList<NemainEvent>();
			for (NemainEvent event : data) {
				if (event.isWithinNDaysAfter(getCurrentDate(), 7)) {
					ret.add(event);
				}
			}
			
			Object[] arrRet = ret.toArray();
			return arrRet;
		}
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
		public void dispose() {}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI controls.
	
	private ImageCache imageCache = new ImageCache();
	protected final int sep = 3;
	
	private Button btnDateBack;
	private Button btnDateForward;
	private Label lblStatus;
	private TableViewer viewer;
	
	private void createControls (Composite parent) {
		FormData formData;
		parent.setLayout(new FormLayout());
		
		Composite tbCom = new Composite(parent, SWT.NONE);
		btnDateBack = new Button(tbCom, SWT.PUSH);
		btnDateForward = new Button(tbCom, SWT.PUSH);
		lblStatus = new Label(tbCom, SWT.NONE);
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
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
		
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(viewer);
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, -sep);
		viewer.getTable().setLayoutData(formData);
		
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
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof NemainEvent) {
					NemainEvent eventToEdit = (NemainEvent) obj;
					
					EditEntryShell editEntryShell = new EditEntryShell(getSite().getShell());
					if (editEntryShell.showDlg(eventToEdit)) {
						String newText = editEntryShell.getExitText();
						
						System.err.println("TODO: save new text: " + newText);
					}
					
				}
			}
		});
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "net.sparktank.nemain.viewer");
	}
	
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
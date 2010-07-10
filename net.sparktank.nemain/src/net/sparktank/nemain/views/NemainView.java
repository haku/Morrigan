package net.sparktank.nemain.views;


import net.sparktank.nemain.config.Config;
import net.sparktank.nemain.model.SqliteLayer;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
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
	
	private SqliteLayer dataSource;
	
	private void initDataSource () throws DbException {
		String fullPathToDb = Config.getFullPathToDb(FILE_CAL_DB);
		dataSource = new SqliteLayer(fullPathToDb);
		System.err.println("Connected to '"+fullPathToDb+"'.");
	}
	
	private void closeDataSource () throws DbException {
		dataSource.dispose();
		System.err.println("Disconnected '"+dataSource.getDbFilePath()+"'.");
	}
	
	private class ViewContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object parent) {
			try {
				return dataSource.getEvents().toArray();
			}
			catch (DbException e) {
				return new String[] {};
			}
		}
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
		public void dispose() {}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI controls.
	
	private TableViewer viewer;
	
	private void createControls (Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setInput(getViewSite());
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "net.sparktank.nemain.viewer");
	}
	
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
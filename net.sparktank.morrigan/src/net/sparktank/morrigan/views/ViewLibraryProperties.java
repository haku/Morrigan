package net.sparktank.morrigan.views;

import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.model.media.MediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;

public class ViewLibraryProperties extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewLibraryProperties";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		addToolbar();
		updateStatus();
	}
	
	@Override
	public void setFocus() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaLibrary library;
	
	public void setContent (MediaLibrary library) {
		this.library = library;
		updateStatus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private Label mainLabel;
	TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		GridData gridData;
		
		parent.setLayout(new GridLayout(1, false));
		
		mainLabel = new Label(parent, SWT.WRAP);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		mainLabel.setLayoutData(gridData);
		
		Table table = new Table(parent, SWT.MULTI | SWT.V_SCROLL );
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		table.setLayoutData(gridData);
		
		tableViewer = new TableViewer(table);    
		tableViewer.setContentProvider(sourcesProvider);
		
	}
	
	private void addToolbar () {
		getViewSite().getActionBars().getToolBarManager().add(addAction);
		getViewSite().getActionBars().getToolBarManager().add(removeAction);
	}
	
	private void updateStatus () {
		if (library!=null) {
			mainLabel.setText("Sources for " + library.getListName() + ".");
			
		} else {
			mainLabel.setText("No library selected.");
		}
		
		tableViewer.refresh();
	}
	
	private IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {
			
			@Override
			public Object[] getElements(Object inputElement) {
				List<String> sources = null;
				
				if (library!=null) {
					try {
						sources = library.getSources();
					} catch (DbException e) {
						new MorriganMsgDlg(e, getSite().getShell().getDisplay());
					}
					
					return sources.toArray();
					
				} else {
					return null;
				}
			}
			
			@Override
			public void dispose() {}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
			
		};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	private IAction addAction = new Action("add", Activator.getImageDescriptor("icons/plus.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement add desu~.").open();
		};
	};
	
	private IAction removeAction = new Action("remove", Activator.getImageDescriptor("icons/minus.gif")) {
		public void run() {
			new MorriganMsgDlg("TODO: implement remove desu~.").open();
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

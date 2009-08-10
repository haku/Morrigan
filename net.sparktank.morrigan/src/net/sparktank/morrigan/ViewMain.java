package net.sparktank.morrigan;

import net.sparktank.morrigan.providers.ViewMainContentProvider;
import net.sparktank.morrigan.providers.ViewMainLabelProvider;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

public class ViewMain extends ViewPart {
	public static final String ID = "net.sparktank.morrigan.ViewMain";
	
	private TableViewer viewer;
	
	/**
	 * This is a callback that will allow us to create the viewer and initialise it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewMainContentProvider());
		viewer.setLabelProvider(new ViewMainLabelProvider());
		viewer.setInput(getViewSite()); // use content provider.
		
		getSite().setSelectionProvider(viewer);
		hookDoubleClickCommand();
	}
	
	private void hookDoubleClickCommand() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
				
				
				System.out.println(event.getSelection().toString());
				
				
//				try {
//					handlerService.executeCommand("net.sparktank.morrigan.handler.CallMediaListEditor", null);
//				} catch (ExecutionException e) {
//					e.printStackTrace();
//				} catch (NotDefinedException e) {
//					e.printStackTrace();
//				} catch (NotEnabledException e) {
//					e.printStackTrace();
//				} catch (NotHandledException e) {
//					e.printStackTrace();
//				}
			}
		});
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
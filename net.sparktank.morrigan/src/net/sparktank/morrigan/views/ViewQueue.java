package net.sparktank.morrigan.views;

import java.util.List;

import net.sparktank.morrigan.model.media.PlayItem;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.ViewPart;

public class ViewQueue extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewQueue";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	AbstractPlayerView abstractPlayerView = null;
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		
		IViewPart findView = getSite().getPage().findView(ViewControls.ID); // FIXME can i find AbstractPlayerView?
		if (findView != null && findView instanceof AbstractPlayerView) {
			abstractPlayerView = (AbstractPlayerView) findView;
			setContent(abstractPlayerView.getQueueList());
			abstractPlayerView.addQueueChangeListener(queueChangedListener);
			queueChangedListener.run();
		}
	}
	
	@Override
	public void setFocus() {}
	
	@Override
	public void dispose() {
		if (abstractPlayerView != null ) {
			abstractPlayerView.removeQueueChangeListener(queueChangedListener);
		}
		
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private List<PlayItem> queue = null;
	
	private void setContent (List<PlayItem> queue) {
		this.queue = queue;
	}
	
	private Runnable queueChangedListener = new Runnable() {
		@Override
		public void run() {
			getSite().getShell().getDisplay().asyncExec(updateGuiRunable);
		}
	};
	
	private Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			if (tableViewer.getTable().isDisposed()) return;
			tableViewer.refresh();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL);
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(getViewSite()); // use content provider.
	}
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			if (queue!=null) {
				return queue.toArray();
				
			} else {
				return new String[]{};
			}
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	private ILabelProvider labelProvider = new ILabelProvider() {
		
		@Override
		public String getText(Object element) {
			if (element instanceof PlayItem) {
				PlayItem item = (PlayItem) element;
				return item.item.toString();
			}
			return null;
		}
		
		@Override
		public Image getImage(Object element) {
			return null;
		}
		
		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		@Override
		public void removeListener(ILabelProviderListener listener) {}
		@Override
		public void dispose() {}
		@Override
		public void addListener(ILabelProviderListener listener) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

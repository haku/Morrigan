package net.sparktank.morrigan.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.media.PlayItem;
import net.sparktank.morrigan.views.AbstractPlayerView.DurationData;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
	
	private volatile boolean isDisposed = false;
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
		isDisposed = true;
		
		if (abstractPlayerView != null ) {
			abstractPlayerView.removeQueueChangeListener(queueChangedListener);
		}
		
		super.dispose();
	}
	
	protected boolean isDisposed () {
		return isDisposed;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private List<PlayItem> queue = null;
	
	private void setContent (List<PlayItem> queue) {
		this.queue = queue;
	}
	
	private Runnable queueChangedListener = new Runnable() {
		@Override
		public void run() {
			if (!updateGuiRunableScheduled) {
				updateGuiRunableScheduled = true;
				getSite().getShell().getDisplay().asyncExec(updateGuiRunable);
			}
		}
	};
	
	private volatile boolean updateGuiRunableScheduled = false;
	
	private Runnable updateGuiRunable = new Runnable() {
		@Override
		public void run() {
			updateGuiRunableScheduled = false;
			if (tableViewer.getTable().isDisposed()) return;
			updateStatus();
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
		
		getViewSite().getActionBars().getToolBarManager().add(moveUpAction);
		getViewSite().getActionBars().getToolBarManager().add(moveDownAction);
		getViewSite().getActionBars().getToolBarManager().add(removeAction);
	}
	
	private void updateStatus () {
		if (isDisposed()) return ;
		
		if (queue.size() == 0) {
			setContentDescription("Queue is empty.");
			
		} else {
			DurationData d = abstractPlayerView.getQueueTotalDuration();
			setContentDescription(
					queue.size() + " items"
					+ " totaling " + (d.complete ? "" : "more than ") +
					TimeHelper.formatTime(d.duration) + "."
			);
		}
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
	
	private ArrayList<PlayItem> getSelectedSources () {
		ISelection selection = tableViewer.getSelection();
		
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			ArrayList<PlayItem> ret = new ArrayList<PlayItem>();
			for (Object selectedObject : iSel.toList()) {
				if (selectedObject != null) {
					if (selectedObject instanceof PlayItem) {
						PlayItem item = (PlayItem) selectedObject;
						ret.add(item);
					}
				}
			}
			return ret;
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	protected IAction moveUpAction = new Action("Move up", Activator.getImageDescriptor("icons/arrow-up.gif")) {
		public void run() {
			abstractPlayerView.moveInQueue(getSelectedSources(), false);
		};
	};
	
	protected IAction moveDownAction = new Action("Move down", Activator.getImageDescriptor("icons/arrow-down.gif")) {
		public void run() {
			abstractPlayerView.moveInQueue(getSelectedSources(), true);
		};
	};
	
	protected IAction removeAction = new Action("Remove", Activator.getImageDescriptor("icons/minus.gif")) {
		@Override
		public void run() {
			ArrayList<PlayItem> selectedSources = getSelectedSources();
			if (selectedSources==null || selectedSources.isEmpty()) {
				return;
			}
			for (PlayItem item : selectedSources) {
				abstractPlayerView.removeFromQueue(item);
			}
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

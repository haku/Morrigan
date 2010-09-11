package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.adaptors.PlayItemLblProv;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.gui.helpers.RefreshTimer;
import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.media.impl.DurationData;
import net.sparktank.morrigan.player.PlayItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.ViewPart;

public class ViewQueue extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewQueue";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private volatile boolean isDisposed = false;
	AbstractPlayerView abstractPlayerView = null;
	
	@Override
	public void createPartControl(Composite parent) {
		makeQueueChangedRrefresher();
		createLayout(parent);
		
		IViewPart findView = getSite().getPage().findView(ViewControls.ID); // FIXME can i find AbstractPlayerView?
		if (findView != null && findView instanceof AbstractPlayerView) {
			this.abstractPlayerView = (AbstractPlayerView) findView;
			setContent(this.abstractPlayerView.getPlayer().getQueueList());
			this.abstractPlayerView.getPlayer().addQueueChangeListener(this.queueChangedRrefresher);
			this.queueChangedRrefresher.run();
		}
	}
	
	@Override
	public void setFocus() {
		this.tableViewer.getTable().setFocus();
	}
	
	@Override
	public void dispose() {
		this.isDisposed = true;
		
		if (this.abstractPlayerView != null ) {
			this.abstractPlayerView.getPlayer().removeQueueChangeListener(this.queueChangedRrefresher);
		}
		
		this.imageCache.clearCache();
		
		super.dispose();
	}
	
	protected boolean isDisposed () {
		return this.isDisposed;
	}
	
	ViewQueue getThis () {
		return this;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	List<PlayItem> queue = null;
	
	private void setContent (List<PlayItem> queue) {
		this.queue = queue;
	}
	
	Runnable queueChangedRrefresher;
	
	private void makeQueueChangedRrefresher () {
		this.queueChangedRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 1000, new Runnable() {
			@Override
			public void run() {
				if (ViewQueue.this.tableViewer.getTable().isDisposed()) return;
				updateStatus();
				ViewQueue.this.tableViewer.refresh();
				bringToTopIfChanged();
			}
		});
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	ImageCache imageCache = new ImageCache();
	TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		this.tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		this.tableViewer.setContentProvider(this.contentProvider);
		this.tableViewer.setLabelProvider(new PlayItemLblProv(this.imageCache));
		this.tableViewer.setInput(getViewSite()); // use content provider.
		this.tableViewer.getTable().addKeyListener(this.keyListener);
		
		getViewSite().getActionBars().getToolBarManager().add(this.moveUpAction);
		getViewSite().getActionBars().getToolBarManager().add(this.moveDownAction);
		getViewSite().getActionBars().getToolBarManager().add(this.removeAction);
	}
	
	private int lastQueueSize = 0;
	
	void bringToTopIfChanged () {
		int size = this.queue.size();
		if (size > this.lastQueueSize) {
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					getSite().getPage().bringToTop(getThis());
				}
			});
		}
		this.lastQueueSize = size;
	}
	
	void updateStatus () {
		if (isDisposed()) return;
		
		if (this.queue.size() == 0) {
			setContentDescription("Queue is empty.");
		}
		else {
			DurationData d = this.abstractPlayerView.getPlayer().getQueueTotalDuration();
			setContentDescription(
					this.queue.size() + " items"
					+ " totaling " + (d.isComplete() ? "" : "more than ") +
					TimeHelper.formatTimeSeconds(d.getDuration()) + "."
			);
		}
	}
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			if (ViewQueue.this.queue!=null) {
				return ViewQueue.this.queue.toArray();
			}
			
			return new String[]{};
		}
		
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
		
	};
	
	private KeyListener keyListener = new KeyListener() {
		
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.keyCode == SWT.DEL) {
				ViewQueue.this.removeAction.run();
			}
		}
		
		@Override
		public void keyPressed(KeyEvent e) {/* UNUSED */}
	};
	
	ArrayList<PlayItem> getSelectedSources () {
		ISelection selection = this.tableViewer.getSelection();
		
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
		@Override
		public void run() {
			ViewQueue.this.abstractPlayerView.getPlayer().moveInQueue(getSelectedSources(), false);
		};
	};
	
	protected IAction moveDownAction = new Action("Move down", Activator.getImageDescriptor("icons/arrow-down.gif")) {
		@Override
		public void run() {
			ViewQueue.this.abstractPlayerView.getPlayer().moveInQueue(getSelectedSources(), true);
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
				ViewQueue.this.abstractPlayerView.getPlayer().removeFromQueue(item);
			}
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

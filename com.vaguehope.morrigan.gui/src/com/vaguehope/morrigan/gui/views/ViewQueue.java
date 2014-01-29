package com.vaguehope.morrigan.gui.views;

import java.util.ArrayList;
import java.util.List;

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

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.adaptors.PlayItemLblProv;
import com.vaguehope.morrigan.gui.helpers.ImageCache;
import com.vaguehope.morrigan.gui.helpers.RefreshTimer;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerLifeCycleListener;
import com.vaguehope.morrigan.util.TimeHelper;

public class ViewQueue extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String ID = "com.vaguehope.morrigan.gui.views.ViewQueue";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private volatile boolean isDisposed = false;
	AbstractPlayerView abstractPlayerView = null;

	@Override
	public void createPartControl(final Composite parent) {
		makeQueueChangedRrefresher();
		createLayout(parent);

		IViewPart findView = getSite().getPage().findView(ViewControls.ID); // FIXME can i find AbstractPlayerView?
		if (findView != null && findView instanceof AbstractPlayerView) {
			this.abstractPlayerView = (AbstractPlayerView) findView;
			this.abstractPlayerView.addPlayerLifeCycleListener(this.playerLifeCycleListener);
		}
	}

	@Override
	public void setFocus() {
		this.tableViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		this.isDisposed = true;
		this.abstractPlayerView.removePlayerLifeCycleListener(this.playerLifeCycleListener);
		this.imageCache.clearCache();
		super.dispose();
	}

	protected boolean isDisposed () {
		return this.isDisposed;
	}

	ViewQueue getThis () {
		return this;
	}

	private final PlayerLifeCycleListener playerLifeCycleListener = new PlayerLifeCycleListener() {

		@Override
		public void playerCreated (final Player player) {
			setContent(ViewQueue.this.abstractPlayerView.getPlayer().getQueue().getQueueList());
			ViewQueue.this.abstractPlayerView.getPlayer().getQueue().addQueueChangeListener(ViewQueue.this.queueChangedRrefresher);
			ViewQueue.this.queueChangedRrefresher.run();
		}

		@Override
		public void playerDisposed (final Player player) {
			if (ViewQueue.this.abstractPlayerView != null ) {
				ViewQueue.this.abstractPlayerView.getPlayer().getQueue().removeQueueChangeListener(ViewQueue.this.queueChangedRrefresher);
			}
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	List<PlayItem> queue = null;

	private void setContent (final List<PlayItem> queue) {
		this.queue = queue;
	}

	RefreshTimer queueChangedRrefresher;

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

	private void createLayout (final Composite parent) {
		this.tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		this.tableViewer.setContentProvider(this.contentProvider);
		this.tableViewer.setLabelProvider(new PlayItemLblProv(this.imageCache));
		this.tableViewer.setInput(getViewSite()); // use content provider.
		this.tableViewer.getTable().addKeyListener(this.keyListener);

		getViewSite().getActionBars().getToolBarManager().add(this.moveUpAction);
		getViewSite().getActionBars().getToolBarManager().add(this.moveDownAction);
		getViewSite().getActionBars().getToolBarManager().add(this.removeAction);
		getViewSite().getActionBars().getToolBarManager().add(this.shuffleAction);
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
			DurationData d = this.abstractPlayerView.getPlayer().getQueue().getQueueTotalDuration();
			setContentDescription(
					this.queue.size() + " items"
					+ " totaling " + (d.isComplete() ? "" : "more than ") +
					TimeHelper.formatTimeSeconds(d.getDuration()) + "."
			);
		}
	}

	private final IStructuredContentProvider contentProvider = new IStructuredContentProvider() {

		@Override
		public Object[] getElements(final Object inputElement) {
			if (ViewQueue.this.queue!=null) {
				return ViewQueue.this.queue.toArray();
			}

			return new String[]{};
		}

		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {/* UNUSED */}

	};

	private final KeyListener keyListener = new KeyListener() {

		@Override
		public void keyReleased(final KeyEvent e) {
			if (e.keyCode == SWT.DEL) {
				ViewQueue.this.removeAction.run();
			}
		}

		@Override
		public void keyPressed(final KeyEvent e) {/* UNUSED */}
	};

	List<PlayItem> getSelectedSources () {
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
			ViewQueue.this.queueChangedRrefresher.reset();
			ViewQueue.this.abstractPlayerView.getPlayer().getQueue().moveInQueue(getSelectedSources(), false);
		}
	};

	protected IAction moveDownAction = new Action("Move down", Activator.getImageDescriptor("icons/arrow-down.gif")) {
		@Override
		public void run() {
			ViewQueue.this.queueChangedRrefresher.reset();
			ViewQueue.this.abstractPlayerView.getPlayer().getQueue().moveInQueue(getSelectedSources(), true);
		}
	};

	protected IAction removeAction = new Action("Remove", Activator.getImageDescriptor("icons/minus.gif")) {
		@Override
		public void run() {
			List<PlayItem> selectedSources = getSelectedSources();
			if (selectedSources==null || selectedSources.isEmpty()) {
				return;
			}
			for (PlayItem item : selectedSources) {
				ViewQueue.this.abstractPlayerView.getPlayer().getQueue().removeFromQueue(item);
			}
		}
	};

	protected IAction shuffleAction = new Action ("Shuffle", Activator.getImageDescriptor("icons/question.png")) {
		@Override
		public void run() {
			ViewQueue.this.abstractPlayerView.getPlayer().getQueue().shuffleQueue();
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

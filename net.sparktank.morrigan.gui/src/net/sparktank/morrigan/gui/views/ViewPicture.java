package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.IMixedMediaItemDbEditor;
import net.sparktank.morrigan.gui.editors.MediaItemDbEditorInput;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.MediaItemListEditorInput;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.IMediaItemDb;
import net.sparktank.morrigan.model.media.IMediaPicture;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

public class ViewPicture extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewPicture";
	
	private static final int PICTURE_CHANGE_INTERVAL = 60000;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		initSelectionListener();
	}
	
	@Override
	public void dispose() {
		stopTimer();
		removeSelectionListener();
		disposeGui();
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	State.
	
	/*
	 * Some very basic code to save / restore local MMDB.
	 */
	
	private static final String KEY_TIMER = "TIMER";
	private static final String KEY_DB = "DB";
	private static final String KEY_ITEM = "ITEM";
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		
		memento.putBoolean(KEY_TIMER, isTimerEnabled());
		
		if (this.editedItemDb != null && this.editedItemDb.getType().equals(LocalMixedMediaDb.TYPE)) {
			memento.putString(KEY_DB, this.editedItemDb.getDbPath());
			memento.putString(KEY_ITEM, this.editedItem.getFilepath());
		}
	}
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		
		if (memento != null) {
			try {
				String dbpath = memento.getString(KEY_DB);
				String itempath = memento.getString(KEY_ITEM);
				
				if (dbpath != null && itempath != null) {
	    			LocalMixedMediaDb mmdb;
	    			mmdb = LocalMixedMediaDb.LOCAL_MMDB_FACTORY.manufacture(dbpath);
	    			mmdb.read();
	    			IMediaPicture item = mmdb.findItemByFilePath(itempath);
	    			
	    			if (item != null) { 
	    				setInput(mmdb, item);
	    			}
	    			else {
	    				// TODO something with this error.
	    				System.err.println("Failed to restore item '"+itempath+"' from '"+dbpath+"'.");
	    			}
				}
			}
			catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
			
			Boolean b = memento.getBoolean(KEY_TIMER);
			if (b == null || b.booleanValue()) { // Default to true.
				startTimer();
		}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	External events.
	
	private void initSelectionListener () {
		ISelectionService service = getSite().getWorkbenchWindow().getSelectionService();
		service.addSelectionListener(this.selectionListener);
	}
	
	private void removeSelectionListener () {
		ISelectionService service = getSite().getWorkbenchWindow().getSelectionService();
		service.removeSelectionListener(this.selectionListener);
	}
	
	volatile boolean listenToSelectionListener = true;
	
	private ISelectionListener selectionListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (ViewPicture.this.listenToSelectionListener && part instanceof IMixedMediaItemDbEditor) {
				if (selection==null || selection.isEmpty()) {
					return;
				}
				
				IMixedMediaItemDbEditor<?,?,?> editor = (IMixedMediaItemDbEditor<?,?,?>) part;
				IMediaItemDb<?,?,? extends IMediaPicture> list = editor.getMediaList();
				
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection iSel = (IStructuredSelection) selection;
					ArrayList<IMediaPicture> sel = new ArrayList<IMediaPicture>();
					for (Object selectedObject : iSel.toList()) {
						if (selectedObject != null) {
							if (selectedObject instanceof IMediaPicture) {
								IMediaPicture track = (IMediaPicture) selectedObject;
								sel.add(track);
							}
						}
					}
					
					setInput(list, sel);
				}
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data links.
	
	IMediaItemDb<?,?,? extends IMediaPicture> editedItemDb = null;
	IMediaPicture editedItem = null;
	
	public void setInput (IMediaItemDb<?,?,? extends IMediaPicture> editedMediaList, List<? extends IMediaPicture> selection) {
		IMediaPicture item = null;
		if (selection != null && selection.size() > 0) {
			if (selection.size() == 1) {
				item = selection.get(0);
			}
		}
		if (item != null && item.isPicture()) { // item will be null if multiple items selected.
			setInput(editedMediaList, item);
		}
	}
	
	public void setInput (IMediaItemDb<?,?,? extends IMediaPicture> editedMediaList, IMediaPicture item) {
		if (this.editedItemDb != editedMediaList || this.editedItem != item) {
			this.editedItem = item;
			
			if (this.editedItem != null) {
				this.editedItemDb = editedMediaList;
			}
			else {
				this.editedItemDb = null;
			}
			
			setPicture(this.editedItem);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	protected final int sep = 3;
	
	Canvas pictureCanvas = null;
	Image pictureImage = null;
	long pictureLastChanged = 0;
	
	private void disposeGui () {
		if (this.pictureImage != null && !this.pictureImage.isDisposed()) {
			this.pictureImage.dispose();
			this.pictureImage = null;
		}
	}
	
	private void createLayout (Composite parent) {
		parent.setLayout(new FillLayout());
		
		this.pictureCanvas = new Canvas(parent, SWT.NONE);
		this.pictureCanvas.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		this.pictureCanvas.addPaintListener(this.picturePainter);
		this.pictureCanvas.addKeyListener(this.keyListener);
		
		getViewSite().getActionBars().getToolBarManager().add(this.prevItemAction);
		getViewSite().getActionBars().getToolBarManager().add(this.nextItemAction);
		getViewSite().getActionBars().getToolBarManager().add(this.randomItemAction);
		getViewSite().getActionBars().getToolBarManager().add(this.enableTimerAction);
		getViewSite().getActionBars().getToolBarManager().add(new Separator());
		getViewSite().getActionBars().getToolBarManager().add(this.revealItemAction);
		getViewSite().getActionBars().getToolBarManager().add(this.showTagsAction);
	}
	
	@Override
	public void setFocus() {
		if (this.pictureCanvas != null && !this.pictureCanvas.isDisposed()) {
			this.pictureCanvas.setFocus();
		}
	}
	
	private KeyListener keyListener = new KeyListener() {
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.PAGE_DOWN
					|| e.character == ' ' || e.character == 'n' || e.character == 'f' || e.character == '\r') {
				nextPicture(1);
			}
			else if (e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.PAGE_UP
					|| e.character == 'p' || e.character == 'b') {
				nextPicture(-1);
			}
			else if (e.character == 'r' || e.character == 'q' || e.character == 'g') {
				randomPicture();
			}
			else if (e.character == 't') {
				try {
					showTagsView();
				}
				catch (Exception ex) {
					new MorriganMsgDlg(ex).open();
				}
			}
		}
		@Override
		public void keyPressed(KeyEvent e) {/* UNUSED */}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Loading and showing pictures.
	
	class LoadPictureJob extends Job {
		
		private final IMediaPicture item;
		private final Display display;
		
		public LoadPictureJob (Display display, IMediaPicture item) {
			super("Loading picture");
			this.display = display;
			this.item = item;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (this.item != null && this.item.isEnabled()) {
				if (ViewPicture.this.pictureImage != null) throw new IllegalArgumentException();
				String filepath = this.item.getFilepath();
				ViewPicture.this.pictureImage = new Image(this.display, filepath);
				new UpdatePictureJob(this.display).schedule();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	class UpdatePictureJob extends UIJob {
		
		public UpdatePictureJob (Display jobDisplay) {
			super(jobDisplay, "Update picture");
		}
		
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			ViewPicture.this.pictureLastChanged = System.currentTimeMillis();
			
			if (!ViewPicture.this.pictureCanvas.isDisposed()) {
				ViewPicture.this.pictureCanvas.redraw();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private void setPicture (final IMediaPicture item) {
		if (item == null) return;
		
		if (item.isPicture()) {
			if (ViewPicture.this.pictureImage != null && !ViewPicture.this.pictureImage.isDisposed()) {
				ViewPicture.this.pictureImage.dispose();
				ViewPicture.this.pictureImage = null;
			}
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				@SuppressWarnings("synthetic-access")
				@Override
				public void run() {
					setContentDescription(item.getTitle() + " (" + item.getWidth() + "x" + item.getHeight() + ")");
				}
			});
			
			LoadPictureJob loadPictureJob = new LoadPictureJob(getSite().getShell().getDisplay(), item);
			
			/*
			 * TODO FIXME use this to prevent two running at once!
			 */
//			loadPictureJob.setRule(new ISchedulingRule() {
//				
//				@Override
//				public boolean isConflicting(ISchedulingRule rule) {
//					return false;
//				}
//				
//				@Override
//				public boolean contains(ISchedulingRule rule) {
//					return false;
//				}
//			});
			
			loadPictureJob.schedule();
		}
	}
	
	private PaintListener picturePainter = new PaintListener () {
		@Override
		public void paintControl(PaintEvent e) {
			if (ViewPicture.this.pictureImage != null) {
				Rectangle srcBounds = ViewPicture.this.pictureImage.getBounds();
				Rectangle dstBounds = ViewPicture.this.pictureCanvas.getClientArea();
				
				double s1 = dstBounds.width / (double)srcBounds.width;
				double s2 = dstBounds.height / (double)srcBounds.height;
				
				int w; int h; int l; int t;
				
				if (s1 < s2) {
					w = (int) (srcBounds.width * s1);
					h = (int) (srcBounds.height * s1);
				}
				else {
					w = (int) (srcBounds.width * s2);
					h = (int) (srcBounds.height * s2);
				}
				
				l = (dstBounds.width / 2) - (w / 2);
				t = (dstBounds.height / 2) - (h / 2);
				
				e.gc.drawImage(ViewPicture.this.pictureImage,
						0, 0, srcBounds.width, srcBounds.height,
						l, t, w, h
				);
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Control methods.
	
	protected void nextPicture (int x) {
		if (this.editedItemDb != null && this.editedItem != null) {
			List<? extends IMediaPicture> dbEntries = this.editedItemDb.getMediaItems();
			int current = dbEntries.indexOf(this.editedItem);
			int res = -1;
			if (current >= 0) { // Did we find the current item?
				int i = current + x;
				while (i != current) { // Keep searching until we find a picture or we get back where we started.
					if (i > dbEntries.size() - 1) {
						i = 0;
					}
					else if (i < 0) {
						i = dbEntries.size() - 1;
					}
					if (dbEntries.get(i).isPicture()) {
						res = i;
						break;
					}
					i = i + x;
				}
			}
			else if (dbEntries.size() > 0) {
				res = 0;
			}
			
			if (res >= 0) {
				IMediaPicture entry = dbEntries.get(res);
				
				// Reveal current item in list?
				IEditorPart activeEditor = getViewSite().getWorkbenchWindow().getActivePage().getActiveEditor();
				if (activeEditor != null) {
					IEditorInput edInput = activeEditor.getEditorInput();
					if (edInput instanceof MediaItemListEditorInput) {
						MediaItemListEditorInput<?> miEdInput = (MediaItemListEditorInput<?>) edInput;
						if (miEdInput.getMediaList().getListId().equals(this.editedItemDb.getListId())) {
							if (activeEditor instanceof MediaItemListEditor<?,?>) {
								MediaItemListEditor<?,?> mediaListEditor = (MediaItemListEditor<?,?>) activeEditor;
								mediaListEditor.revealItem(entry, false);
							}
						}
					}
				}
				
				setInput(this.editedItemDb, entry);
			}
		}
	}
	
	protected void randomPicture () {
		if (this.editedItemDb != null && this.editedItem != null) {
			IMediaPicture item = getRandomItem(this.editedItemDb.getMediaItems(), this.editedItem);
			setInput(this.editedItemDb, item);
		}
	}
	
	protected void revealItemInList () throws PartInitException, MorriganException {
		if (this.editedItemDb != null && this.editedItem != null) {
			if (this.editedItemDb.getType().equals(LocalMixedMediaDb.TYPE)) {
				this.listenToSelectionListener = false;
				try {
    				MediaItemDbEditorInput input = EditorFactory.getMmdbInput(this.editedItemDb.getListId());
    				getViewSite().getWorkbenchWindow().getActivePage().openEditor(input, LocalMixedMediaDbEditor.ID);
				} finally {
					this.listenToSelectionListener = true;
				}
			}
			IEditorPart activeEditor = getViewSite().getWorkbenchWindow().getActivePage().getActiveEditor();
			if (activeEditor instanceof MediaItemListEditor<?,?>) {
				MediaItemListEditor<?,?> mediaListEditor = (MediaItemListEditor<?,?>) activeEditor;
				mediaListEditor.revealItem(this.editedItem);
			}
		}
	}
	
	protected void showTagsView () throws PartInitException {
		if (this.editedItemDb != null && this.editedItem != null) {
			IViewPart showView = getSite().getPage().showView(ViewTagEditor.ID);
			ViewTagEditor viewTagEd = (ViewTagEditor) showView;
			viewTagEd.setInput(this.editedItemDb, this.editedItem);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Scheduler.
	
	private Timer timer = new Timer();
	private AtomicReference<TimerTask> changeTimer = new AtomicReference<TimerTask>();
	
	void startTimer () {
		ChangeTask t = new ChangeTask();
		if (this.changeTimer.compareAndSet(null, t)) {
			this.timer.schedule(t, 5000, 5000);
			this.enableTimerAction.setChecked(true);
		}
	}
	
	void stopTimer () {
		TimerTask t = this.changeTimer.get();
		if (t != null && this.changeTimer.compareAndSet(t, null)) {
			t.cancel();
			this.enableTimerAction.setChecked(false);
		}
	}
	
	boolean isTimerEnabled () {
		return this.changeTimer.get() != null;
	}
	
	private class ChangeTask extends TimerTask {

		long lastChaged = 0;
		
		public ChangeTask () {/* UNUSED */}
		
		@Override
		public void run() {
			if (this.lastChaged != ViewPicture.this.pictureLastChanged
					&& System.currentTimeMillis() - ViewPicture.this.pictureLastChanged >= PICTURE_CHANGE_INTERVAL) {
				this.lastChaged = ViewPicture.this.pictureLastChanged;
				randomPicture();
			}
//			else {
//				System.err.println("Picture change in " + (PICTURE_CHANGE_INTERVAL - (System.currentTimeMillis() - ViewPicture.this.pictureLastChanged)));
//			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions.
	
	protected IAction nextItemAction = new Action ("Next", Activator.getImageDescriptor("icons/next.gif")) {
		@Override
		public void run() {
			nextPicture(1);
		};
	};
	
	protected IAction prevItemAction = new Action ("Previous", Activator.getImageDescriptor("icons/prev.gif")) {
		@Override
		public void run() {
			nextPicture(-1);
		};
	};
	
	protected IAction revealItemAction = new Action ("Reveal", Activator.getImageDescriptor("icons/jumptolist.active.gif")) {
		@Override
		public void run() {
			try {
				revealItemInList();
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		};
	};
	
	protected IAction randomItemAction = new Action ("Random", Activator.getImageDescriptor("icons/question.png")) {
		@Override
		public void run() {
			randomPicture();
		};
	};
	
	protected class EnableTimerAction extends Action {
		
		public EnableTimerAction () {
			super("Timer", AS_CHECK_BOX);
			this.setImageDescriptor(Activator.getImageDescriptor("icons/timer.png"));
		}
		
	}
	
	protected IAction enableTimerAction = new EnableTimerAction () {
		@Override
		public void run() {
			if (isChecked()) startTimer(); else stopTimer();
		};
	};
	
	protected IAction showTagsAction = new Action("Tags", Activator.getImageDescriptor("icons/tag.png")) {
		@Override
		public void run () {
			try {
				showTagsView();
			}
			catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static IMediaPicture getRandomItem (List<? extends IMediaPicture> dbEntries, IMediaPicture current) {
		List<IMediaPicture> list = new LinkedList<IMediaPicture>();
		for (IMediaPicture mi : dbEntries) {
			if (mi.isEnabled() && !mi.isMissing() && mi != current && mi.isPicture()) {
				list.add(mi);
			}
		}
		if (list.size() < 1) return null;
		
		Random generator = new Random();
		int x = Math.round(generator.nextFloat() * list.size());
		return list.get(x);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

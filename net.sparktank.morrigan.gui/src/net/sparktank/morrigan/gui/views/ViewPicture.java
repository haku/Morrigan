package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.IMediaItemDbEditor;
import net.sparktank.morrigan.gui.editors.MediaItemDbEditorInput;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.MediaItemListEditorInput;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;

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
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

public class ViewPicture extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewPicture";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		initFileTypes();
		createLayout(parent);
		initSelectionListener();
	}
	
	@Override
	public void dispose() {
		removeSelectionListener();
		disposeGui();
		super.dispose();
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
			if (ViewPicture.this.listenToSelectionListener && part instanceof IMediaItemDbEditor) {
				if (selection==null || selection.isEmpty()) {
					return;
				}
				
				IMediaItemDbEditor<?,?,?> editor = (IMediaItemDbEditor<?,?,?>) part;
				IMediaItemDb<?,?,?> list = editor.getMediaList();
				
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection iSel = (IStructuredSelection) selection;
					ArrayList<IMediaItem> sel = new ArrayList<IMediaItem>();
					for (Object selectedObject : iSel.toList()) {
						if (selectedObject != null) {
							if (selectedObject instanceof IMediaItem) {
								IMediaItem track = (IMediaItem) selectedObject;
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
	
	IMediaItemDb<?,?,? extends IMediaItem> editedItemDb = null;
	IMediaItem editedItem = null;
	
	public void setInput (IMediaItemDb<?,?,? extends IMediaItem> editedMediaList, List<? extends IMediaItem> selection) {
		IMediaItem item = null;
		if (selection != null && selection.size() > 0) {
			if (selection.size() == 1) {
				item = selection.get(0);
			}
		}
		if (isPictureItem(item)) {
			setInput(editedMediaList, item);
		}
	}
	
	public void setInput (IMediaItemDb<?,?,? extends IMediaItem> editedMediaList, IMediaItem item) {
		this.editedItem = item;
		
		if (this.editedItem != null) {
			this.editedItemDb = editedMediaList;
		}
		else {
			this.editedItemDb = null;
		}
		
		setPicture(this.editedItem);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	List<String> supportedFormats = null;
	
	protected final int sep = 3;
	
	Canvas pictureCanvas = null;
	Image pictureImage = null;
	
	private void disposeGui () {
		if (this.pictureImage != null && !this.pictureImage.isDisposed()) {
			this.pictureImage.dispose();
			this.pictureImage = null;
		}
	}
	
	public void initFileTypes () {
		try {
			this.supportedFormats = Arrays.asList(Config.getPictureFileTypes());
		} catch (MorriganException e) {
			throw new RuntimeException(e);
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
		getViewSite().getActionBars().getToolBarManager().add(new Separator());
		getViewSite().getActionBars().getToolBarManager().add(this.revealItemAction);
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
		}
		@Override
		public void keyPressed(KeyEvent e) {/* UNUSED */}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Loading and showing pictures.
	
	class LoadPictureJob extends Job {
		
		private final IMediaItem item;
		private final Display display;

		public LoadPictureJob (Display display, IMediaItem item) {
			super("Loading picture");
			this.display = display;
			this.item = item;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (this.item != null && this.item.isEnabled()) {
				if (ViewPicture.this.pictureImage != null) throw new IllegalArgumentException();
				ViewPicture.this.pictureImage = new Image(ViewPicture.this.pictureCanvas.getDisplay(), this.item.getFilepath());
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
			if (!ViewPicture.this.pictureCanvas.isDisposed()) {
				ViewPicture.this.pictureCanvas.redraw();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private void setPicture (IMediaItem item) {
		if (item == null) return;
		
		if (isPictureItem(item)) {
			if (ViewPicture.this.pictureImage != null && !ViewPicture.this.pictureImage.isDisposed()) {
				ViewPicture.this.pictureImage.dispose();
				ViewPicture.this.pictureImage = null;
			}
			setContentDescription(item.getTitle());
			
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
			List<? extends IMediaItem> dbEntries = this.editedItemDb.getMediaItems();
			int i = dbEntries.indexOf(this.editedItem);
			if (i >= 0) { // Did we find the current item?
				i = i + x;
				if (i > dbEntries.size() -1) {
					i = 0;
				}
				else if (i < 0) {
					i = dbEntries.size() - 1;
				}
			} else {
				i = 0;
			}
			IMediaItem entry = dbEntries.get(i);
			
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean isPictureItem (IMediaItem item) {
		String ext = item.getFilepath();
		ext = ext.substring(ext.lastIndexOf(".") + 1).toLowerCase();
		return (this.supportedFormats.contains(ext));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

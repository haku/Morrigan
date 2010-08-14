package net.sparktank.morrigan.gui.views;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.display.DropMenuListener;
import net.sparktank.morrigan.gui.editors.IMediaItemDbEditor;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.media.impl.MediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.morrigan.model.tags.TrackTagHelper;
import net.sparktank.morrigan.model.tracks.MediaTrack;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class ViewTagEditor extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.views.ViewTagEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ImageCache imageCache = new ImageCache();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createPartControl(Composite parent) {
		createLayout(parent);
		addPartListener();
		initSelectionListener();
	}
	
	@Override
	public void setFocus() {
		this.txtNewTag.setFocus();
	}
	
	@Override
	public void dispose() {
		removeSelectionListener();
		removePartListener();
		this.imageCache.clearCache();
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
	
	private void addPartListener () {
		getViewSite().getPage().addPartListener(this.partListener);
	}
	
	private void removePartListener () {
		getViewSite().getPage().removePartListener(this.partListener);
	}
	
	private IPartListener partListener = new IPartListener() {
		@Override
		public void partClosed(IWorkbenchPart part) {
			if (part instanceof IMediaItemDbEditor) {
				IMediaItemDbEditor libEditor = (IMediaItemDbEditor) part;
				if (libEditor.getMediaList().equals(ViewTagEditor.this.editedItemDb)) {
					setInput(null, null);
				}
			}
		}
		
		@Override
		public void partActivated(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partOpened(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partDeactivated(IWorkbenchPart part) {/* UNUSED */}
		@Override
		public void partBroughtToTop(IWorkbenchPart part) {/* UNUSED */}
	};
	
	private ISelectionListener selectionListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (part instanceof IMediaItemDbEditor) {
				if (selection==null || selection.isEmpty()) {
					setInput(null, null);
					return;
				}
				
				IMediaItemDbEditor editor = (IMediaItemDbEditor) part;
				IMediaItemDb<?,?> list = editor.getMediaList();
				
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection iSel = (IStructuredSelection) selection;
					ArrayList<MediaItem> sel = new ArrayList<MediaItem>();
					for (Object selectedObject : iSel.toList()) {
						if (selectedObject != null) {
							if (selectedObject instanceof MediaItem) {
								MediaItem track = (MediaItem) selectedObject;
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
	
	IMediaItemDb<?,?> editedItemDb = null;
	IMediaItem editedItem = null;
	
	public void setInput (IMediaItemDb<?,?> editedMediaList, List<? extends IMediaItem> selection) {
		if (selection != null && selection.size() > 0) {
			if (selection.size() == 1) {
				setContentDescription(selection.get(0).getTitle());
				this.editedItem = selection.get(0);
			}
			else {
				setContentDescription(selection.size() + " items selected.");
				this.editedItem = null;
			}
		}
		else {
			setContentDescription("No items selected.");
			this.editedItem = null;
		}
		
		this.btnAddTag.setEnabled(this.editedItem != null);
		this.btnRemoveTag.setEnabled(this.editedItem != null);
		this.readTagsAction.setEnabled(this.editedItem != null);
		
		this.editedItemDb = editedMediaList;
		this.tableViewer.refresh();
	}
	
	private IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			if (ViewTagEditor.this.editedItemDb != null && ViewTagEditor.this.editedItem != null) {
				try {
					List<MediaTag> tags = ViewTagEditor.this.editedItemDb.getTags(ViewTagEditor.this.editedItem);
					return tags.toArray();
				}
				catch (MorriganException e) {
					getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
					// TODO disable stuff 'cos we are broken?
					return new String[]{};
				}
			}
			
			return new String[]{};
		}
		
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	protected final int sep = 3;
	
	Text txtNewTag;
	private Button btnAddTag;
	private Button btnRemoveTag;
	private Button btnMenu;
	private TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		FormData formData;
		
		parent.setLayout(new FormLayout());
		
		Composite tbCom = new Composite(parent, SWT.NONE);
		this.txtNewTag = new Text(tbCom, SWT.SINGLE | SWT.BORDER);
		this.btnAddTag = new Button(tbCom, SWT.PUSH);
		this.btnRemoveTag = new Button(tbCom, SWT.PUSH);
		this.btnMenu = new Button(tbCom, SWT.PUSH);
		MenuManager menuMenuMgr = new MenuManager();
		this.tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		
		tbCom.setLayout(new FormLayout());
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		tbCom.setLayoutData(formData);
		
		this.txtNewTag.setMessage("New tag");
		formData = new FormData();
		formData.left = new FormAttachment(0, this.sep);
		formData.right = new FormAttachment(this.btnAddTag, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.txtNewTag.setLayoutData(formData);
		
		this.btnAddTag.setImage(this.imageCache.readImage("icons/plus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(this.btnRemoveTag, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnAddTag.setLayoutData(formData);
		
		this.btnRemoveTag.setImage(this.imageCache.readImage("icons/minus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(this.btnMenu, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnRemoveTag.setLayoutData(formData);
		
		this.btnMenu.setImage(this.imageCache.readImage("icons/pref.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(100, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnMenu.setLayoutData(formData);
		
		this.tableViewer.setContentProvider(this.sourcesProvider);
		this.tableViewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(this.tableViewer);
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.tableViewer.getTable().setLayoutData(formData);
		
		this.txtNewTag.addListener (SWT.DefaultSelection, new Listener () {
			@Override
			public void handleEvent (Event e) {
				procAddTag();
			}
		});
		
		this.txtNewTag.addListener (SWT.FOCUSED, new Listener() {
			@Override
			public void handleEvent(Event event) {
				ViewTagEditor.this.txtNewTag.setSelection(0, ViewTagEditor.this.txtNewTag.getText().length());
			}
		});
		
		this.btnAddTag.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				procAddTag();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
		});
		
		this.btnRemoveTag.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				procRemoveTag();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {/* UNUSED */}
		});
		
		menuMenuMgr.add(this.readTagsAction);
		this.btnMenu.addSelectionListener(new DropMenuListener(this.btnMenu, menuMenuMgr));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected IAction readTagsAction = new Action("Read tags from file", Activator.getImageDescriptor("icons/open.gif")) {
		@Override
		public void run() {
			procReadTags();
		};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	void procAddTag() {
		if (this.editedItemDb != null && this.editedItem != null) {
			String text = this.txtNewTag.getText();
			if (text.length() > 0) {
				try {
					this.editedItemDb.addTag(this.editedItem, text, MediaTagType.MANUAL, (MediaTagClassification)null);
					this.tableViewer.refresh();
					this.txtNewTag.setSelection(0, text.length());
					this.txtNewTag.setFocus();
				}
				catch (MorriganException e) {
					getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
				}
			}
			
		}
		else {
			new MorriganMsgDlg("No item selected to add tag to.").open();
		}
	}
	
	void procRemoveTag() {
		List<MediaTag> selMts = new LinkedList<MediaTag>();
		
		ISelection selection = this.tableViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			
			if (iSel.size() < 1) {
				return;
			}
			
			for (Object selObj : iSel.toList()) {
				if (selObj instanceof MediaTag) {
					MediaTag selMt = (MediaTag) selObj;
					selMts.add(selMt);
				}
			}
		}
		
		MorriganMsgDlg dlg = new MorriganMsgDlg("Remove "+selMts.size()+" selected tags?", MorriganMsgDlg.YESNO);
		dlg.open();
		if (dlg.getReturnCode() == Window.OK) {
			try {
				for (MediaTag mt : selMts) {
					this.editedItemDb.removeTag(mt);
				}
			}
			catch (MorriganException e) {
				getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
			}
			this.tableViewer.refresh();
		}
	}
	
	void procReadTags() {
		if (this.editedItemDb != null && this.editedItem != null) {
			File file = new File(this.editedItem.getFilepath());
			if (file.exists()) {
				
				if (this.editedItem instanceof MediaTrack) {
					MediaTrack mt = (MediaTrack) this.editedItem;
					try {
						TrackTagHelper.readTrackTags(this.editedItemDb, mt, file);
					}
					catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return;
					}
					this.tableViewer.refresh();
				} else {
					new MorriganMsgDlg("TODO: implement this.").open();
				}
				
			}
			else {
				new MorriganMsgDlg("File '"+file.getAbsolutePath()+"' does not exist.").open();
			}
		}
		else {
			new MorriganMsgDlg("No item selected to read tags for.").open();
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

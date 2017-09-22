package com.vaguehope.morrigan.gui.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.dialogs.RunnableDialog;
import com.vaguehope.morrigan.gui.editors.IMediaItemDbEditor;
import com.vaguehope.morrigan.gui.helpers.ImageCache;
import com.vaguehope.morrigan.gui.helpers.RefreshTimer;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;

public class ViewTagEditor extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String ID = "com.vaguehope.morrigan.gui.views.ViewTagEditor";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final ImageCache imageCache = new ImageCache();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void createPartControl (final Composite parent) {
		createLayout(parent);
		addPartListener();
		initSelectionListener();
		makeRefresher();
		applySavedState();
	}

	@Override
	public void setFocus () {
		this.txtNewTag.setSelection(0, this.txtNewTag.getText().length());
		this.txtNewTag.setFocus();
	}

	@Override
	public void dispose () {
		removeSelectionListener();
		removePartListener();
		this.imageCache.clearCache();
		super.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	State.

	private static final String KEY_NEWTAG = "KEY_NEWTAG";
	private String initialNewTag = null;

	/**
	 * Called BEFORE createPartControl().
	 */
	@Override
	public void init (final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		if (memento != null) {
			this.initialNewTag = memento.getString(KEY_NEWTAG);
		}
	}

	@Override
	public void saveState (final IMemento memento) {
		memento.putString(KEY_NEWTAG, this.txtNewTag.getText());

		super.saveState(memento);
	}

	/**
	 * Because init() is called before createPartControl()
	 * this is used to apply the state we read in init();
	 */
	private void applySavedState () {
		if (this.initialNewTag != null) this.txtNewTag.setText(this.initialNewTag);
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

	private final IPartListener partListener = new IPartListener() {
		@Override
		public void partClosed (final IWorkbenchPart part) {
			if (part instanceof IMediaItemDbEditor) {
				IMediaItemDbEditor<?, ?> libEditor = (IMediaItemDbEditor<?, ?>) part;
				if (libEditor.getMediaList().equals(ViewTagEditor.this.editedItemDb)) {
					setInput(null, (IMediaItem) null);
				}
			}
		}

		@Override
		public void partActivated (final IWorkbenchPart part) {/* UNUSED */}

		@Override
		public void partOpened (final IWorkbenchPart part) {/* UNUSED */}

		@Override
		public void partDeactivated (final IWorkbenchPart part) {/* UNUSED */}

		@Override
		public void partBroughtToTop (final IWorkbenchPart part) {/* UNUSED */}
	};

	private final ISelectionListener selectionListener = new ISelectionListener() {
		@Override
		public void selectionChanged (final IWorkbenchPart part, final ISelection selection) {
			if (part instanceof IMediaItemDbEditor) {
				if (selection == null || selection.isEmpty()) {
					setInput(null, (IMediaItem) null);
					return;
				}

				IMediaItemDbEditor<?, ?> editor = (IMediaItemDbEditor<?, ?>) part;
				IMediaItemDb<?, ?> list = editor.getMediaList();

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

	IMediaItemDb<?, ?> editedItemDb = null;
	IMediaItem editedItem = null;
	List<IMediaItem> editedItems = null;

	public void setInput (final IMediaItemDb<?, ?> editedMediaList, final IMediaItem item) {
		if (item != null) {
			List<IMediaItem> list = new ArrayList<IMediaItem>();
			list.add(item);
			setInput(editedMediaList, list);
		}
		else {
			setInput(editedMediaList, (List<? extends IMediaItem>) null);
		}
	}

	public void setInput (final IMediaItemDb<?, ?> editedMediaList, final List<? extends IMediaItem> selection) {
		if (selection != null && selection.size() > 0) {
			if (selection.size() == 1) {
				setContentDescription(selection.get(0).getTitle());
				this.editedItem = selection.get(0);
				this.editedItems = new ArrayList<IMediaItem>(selection);
			}
			else {
				setContentDescription(selection.size() + " items selected.");
				this.editedItem = null;
				this.editedItems = new ArrayList<IMediaItem>(selection);
			}
		}
		else {
			setContentDescription("No items selected.");
			this.editedItem = null;
			this.editedItems = null;
		}

		this.btnAddTag.setEnabled(this.editedItem != null || (this.editedItems != null && this.editedItems.size() > 1));
		this.btnRemoveTag.setEnabled(this.editedItem != null);
		this.readTagsAction.setEnabled(this.editedItem != null);

		this.editedItemDb = editedMediaList;
		this.tableViewer.refresh();
	}

	private final IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {

		@Override
		public Object[] getElements (final Object inputElement) {
			if (ViewTagEditor.this.editedItemDb != null && ViewTagEditor.this.editedItem != null) {
				try {
					if (ViewTagEditor.this.editedItemDb.hasTags(ViewTagEditor.this.editedItem)) {
						List<MediaTag> tags = ViewTagEditor.this.editedItemDb.getTags(ViewTagEditor.this.editedItem);
						return tags.toArray();
					}
				}
				catch (MorriganException e) {
					getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
					// TODO disable stuff 'cos we are broken?
					return new String[] {};
				}
			}

			return new String[] {};
		}

		@Override
		public void dispose () {/* UNUSED */}

		@Override
		public void inputChanged (final Viewer viewer, final Object oldInput, final Object newInput) {/* UNUSED */}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.

	protected final int sep = 3;

	Text txtNewTag;
	private Button btnAddTag;
	private Button btnRemoveTag;
	TableViewer tableViewer;

	Runnable tagsChangedRrefresher;

	private void createLayout (final Composite parent) {
		getViewSite().getActionBars().getMenuManager().add(this.readTagsAction);

		FormData formData;
		parent.setLayout(new FormLayout());

		Composite tbCom = new Composite(parent, SWT.NONE);
		this.txtNewTag = new Text(tbCom, SWT.SINGLE | SWT.BORDER);
		this.btnAddTag = new Button(tbCom, SWT.PUSH);
		this.btnRemoveTag = new Button(tbCom, SWT.PUSH);
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
		formData.right = new FormAttachment(100, -this.sep);
		formData.top = new FormAttachment(0, this.sep);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.btnRemoveTag.setLayoutData(formData);

		this.tableViewer.setContentProvider(this.sourcesProvider);
		this.tableViewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(this.tableViewer);
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, -this.sep);
		this.tableViewer.getTable().setLayoutData(formData);

		this.txtNewTag.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent (final Event e) {
				procAddTag();
			}
		});

		this.txtNewTag.addListener(SWT.FOCUSED, new Listener() {
			@Override
			public void handleEvent (final Event event) {
				ViewTagEditor.this.txtNewTag.setSelection(0, ViewTagEditor.this.txtNewTag.getText().length());
			}
		});

		this.btnAddTag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (final SelectionEvent event) {
				procAddTag();
			}
		});

		this.btnRemoveTag.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected (final SelectionEvent event) {
				procRemoveTag();
			}
		});
	}

	private void makeRefresher () {
		this.tagsChangedRrefresher = new RefreshTimer(getSite().getShell().getDisplay(), 5000, new Runnable() {
			@Override
			public void run () {
				ViewTagEditor.this.tableViewer.refresh();
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	External API methods.

//	public void addToolbarButton (String id, String label) {
//		TODO
//	}

//	public void removeToolbarButton (String id, String label) {
//		TODO
//	}

	public IMediaItemDb<?, ?> getEditedItemDb () {
		return this.editedItemDb;
	}

	public IMediaItem getEditedItem () {
		return this.editedItem;
	}

	public List<IMediaItem> getEditedItems () {
		return this.editedItems;
	}

	public void refreshContent () {
		this.tagsChangedRrefresher.run();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected IAction readTagsAction = new Action("Read tags from file", Activator.getImageDescriptor("icons/open.gif")) {
		@Override
		public void run () {
			procReadTags();
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void procAddTag () {
		final String text = this.txtNewTag.getText();
		if (text.length() <= 0) return;

		if (this.editedItemDb == null) {
			new MorriganMsgDlg("No DB selected.").open();
			return;
		}

		if (this.editedItem == null && (this.editedItems == null || this.editedItems.size() <= 1)) {
			new MorriganMsgDlg("No items selected.").open();
			return;
		}

		try {
			boolean refreshRequired = false;

			if (this.editedItems != null && this.editedItems.size() > 1) { // Bulk action.
				final MorriganMsgDlg dlg = new MorriganMsgDlg(
						String.format("Add tag '%s' to %s items?", text, this.editedItems.size()),
						MorriganMsgDlg.YESNO);
				dlg.open();
				if (dlg.getReturnCode() == Window.OK) {
					for (final IMediaItem item : this.editedItems) {
						this.editedItemDb.addTag(item, text, MediaTagType.MANUAL, (MediaTagClassification) null);
					}
					refreshRequired = true;
				}
			}
			else if (this.editedItem != null) { // Single action.
				this.editedItemDb.addTag(this.editedItem, text, MediaTagType.MANUAL, (MediaTagClassification) null);
				refreshRequired = true;
			}

			if (refreshRequired) {
				this.tableViewer.refresh();
				this.txtNewTag.setSelection(0, text.length());
				this.txtNewTag.setFocus();
			}
		}
		catch (MorriganException e) {
			getSite().getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}

	void procRemoveTag () {
		List<MediaTag> selMts = new ArrayList<MediaTag>();

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

		MorriganMsgDlg dlg = new MorriganMsgDlg("Remove " + selMts.size() + " selected tags?", MorriganMsgDlg.YESNO);
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

	void procReadTags () {
		if (this.editedItemDb != null && this.editedItem != null) {
			File file = new File(this.editedItem.getFilepath());
			if (file.exists()) {

				if (this.editedItem instanceof IMediaTrack) {
					IMediaTrack mt = (IMediaTrack) this.editedItem;
					try {
						Activator.getMediaFactory().readTrackTags(this.editedItemDb, mt, file);
					}
					catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return;
					}
					this.tableViewer.refresh();
				}
				else {
					new MorriganMsgDlg("TODO: implement this.").open();
				}

			}
			else {
				new MorriganMsgDlg("File '" + file.getAbsolutePath() + "' does not exist.").open();
			}
		}
		else {
			new MorriganMsgDlg("No item selected to read tags for.").open();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

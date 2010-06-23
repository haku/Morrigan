package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.gui.editors.AbstractLibraryEditor;
import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.MediaTrack;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
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
		initSelectionListener();
	}
	
	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}
	
	@Override
	public void dispose() {
		removeSelectionListener();
		imageCache.clearCache();
		super.dispose();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	protected final int sep = 3;
	
	private Text txtFilter;
	private Button btnAddTag;
	private Button btnRemoveTag;
	private TableViewer tableViewer;
	
	private void createLayout (Composite parent) {
		FormData formData;
		
		parent.setLayout(new FormLayout());
		
		Composite tbCom = new Composite(parent, SWT.NONE);
		txtFilter = new Text(tbCom, SWT.SINGLE | SWT.BORDER);
		btnAddTag = new Button(tbCom, SWT.PUSH);
		btnRemoveTag = new Button(tbCom, SWT.PUSH);
		
		tbCom.setLayout(new FormLayout());
		formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		tbCom.setLayoutData(formData);
		
		txtFilter.setMessage("New tag");
		formData = new FormData();
		formData.left = new FormAttachment(0, sep);
		formData.right = new FormAttachment(btnAddTag, -sep);
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		txtFilter.setLayoutData(formData);
		
		btnAddTag.setImage(imageCache.readImage("icons/plus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(btnRemoveTag, -sep);
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		btnAddTag.setLayoutData(formData);
		
		btnRemoveTag.setImage(imageCache.readImage("icons/minus.gif"));
		formData = new FormData();
		formData.right = new FormAttachment(100, -sep);
		formData.top = new FormAttachment(0, sep);
		formData.bottom = new FormAttachment(100, -sep);
		btnRemoveTag.setLayoutData(formData);
		
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.V_SCROLL);
		tableViewer.setContentProvider(sourcesProvider);
		tableViewer.setInput(getViewSite()); // use content provider.
		getSite().setSelectionProvider(tableViewer);
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(tbCom, 0);
		formData.bottom = new FormAttachment(100, -sep);
		tableViewer.getTable().setLayoutData(formData);
	}
	
	private IStructuredContentProvider sourcesProvider = new IStructuredContentProvider() {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return new String[]{"tag0", "tag1", "tag2"};
		}
		
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	};
	
	private void inputSelectionChanged (List<MediaTrack> selection) {
		if (selection.size() == 1) {
			setContentDescription(selection.get(0).getTitle());
		}
		else if (selection.size() > 1) {
			setContentDescription(selection.size() + " items selected.");
		}
		else {
			setContentDescription("No items selected.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.
	
	private void initSelectionListener () {
		ISelectionService service = getSite().getWorkbenchWindow().getSelectionService();
		service.addSelectionListener(selectionListener);
	}
	
	private void removeSelectionListener () {
		ISelectionService service = getSite().getWorkbenchWindow().getSelectionService();
		service.removeSelectionListener(selectionListener);
	}
	
	private ISelectionListener selectionListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (selection==null) return;
			if (selection.isEmpty()) return;
			
			System.err.println("selectionChanged("+part.getTitle()+","+selection.toString()+").");
			
			if (part instanceof AbstractLibraryEditor<?>) {
//				AbstractLibraryEditor<?> libEditor = (AbstractLibraryEditor<?>) part;
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection iSel = (IStructuredSelection) selection;
					
					ArrayList<MediaTrack> sel = new ArrayList<MediaTrack>();
					for (Object selectedObject : iSel.toList()) {
						if (selectedObject != null) {
							if (selectedObject instanceof MediaTrack) {
								MediaTrack track = (MediaTrack) selectedObject;
								sel.add(track);
							}
						}
					}
					inputSelectionChanged(sel);
				}
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

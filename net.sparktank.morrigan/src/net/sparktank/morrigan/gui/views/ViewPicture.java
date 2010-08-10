package net.sparktank.morrigan.gui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.editors.IMediaItemDbEditor;
import net.sparktank.morrigan.model.IMediaItemDb;
import net.sparktank.morrigan.model.MediaItem;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

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
	public void setFocus() {/* UNUSED */}
	
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
	MediaItem editedItem = null;
	
	public void setInput (IMediaItemDb<?,?> editedMediaList, List<? extends MediaItem> selection) {
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
		
		showPicture(this.editedItem);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI stuff.
	
	private List<String> supportedFormats = null;
	
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
		this.pictureCanvas.setLayout(new FillLayout());
		this.pictureCanvas.addPaintListener(new PaintListener () {
			@Override
			public void paintControl(PaintEvent e) {
				if (ViewPicture.this.pictureImage != null) {
					Rectangle srcBounds = ViewPicture.this.pictureImage.getBounds();
					Rectangle dstBounds = ViewPicture.this.pictureCanvas.getClientArea();
					e.gc.drawImage(ViewPicture.this.pictureImage,
							0, 0, srcBounds.width, srcBounds.height,
							0, 0, dstBounds.width, dstBounds.height
					);
				}
			}
		});
	}
	
	private void showPicture (MediaItem item) {
		if (item != null) {
			String ext = item.getFilepath();
			ext = ext.substring(ext.lastIndexOf(".") + 1).toLowerCase();
			if (this.supportedFormats.contains(ext)) {
				if (this.pictureImage != null && !this.pictureImage.isDisposed()) {
					this.pictureImage.dispose();
					this.pictureImage = null;
				}
				this.pictureImage = new Image(this.pictureCanvas.getDisplay(), item.getFilepath());
				this.pictureCanvas.redraw();
			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

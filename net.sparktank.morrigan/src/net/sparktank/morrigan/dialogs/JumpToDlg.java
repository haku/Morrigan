package net.sparktank.morrigan.dialogs;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;

import net.sparktank.morrigan.library.DbException;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.media.PlayItem;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class JumpToDlg extends Dialog {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = -6047405753309652452L;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static volatile Boolean dlgOpen = false;
	
	private final MediaLibrary mediaLibrary;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public JumpToDlg (Shell parent, MediaLibrary mediaLibrary) {
		super(parent, SWT.TITLE | SWT.CLOSE | SWT.APPLICATION_MODAL | SWT.RESIZE);
		
		if (mediaLibrary == null) throw new IllegalArgumentException("mediaLibrary can not be null.");
		
		this.mediaLibrary = mediaLibrary;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static int SEP = 3;
	
	private Label label;
	private Text text;
	private TableViewer tableViewer;
	private PlayItem returnValue = null;
	
	public PlayItem open () {
		synchronized (dlgOpen) {
			if (dlgOpen) return null;
			dlgOpen = true;
		}
		
		// Set dlg text.
		setText("Jump to track");
		
		// Create window.
		final Shell shell = new Shell(getParent().getDisplay(), getStyle());
		shell.setImage(getParent().getImage());
		shell.setText(getText());
		
		// Create form layout.
		FormData formData;
		shell.setLayout(new FormLayout());
		
		label = new Label(shell, SWT.CENTER);
		text = new Text(shell, SWT.SINGLE | SWT.BORDER);
		tableViewer =  new TableViewer(shell, SWT.V_SCROLL);
		Button btnOk = new Button(shell, SWT.PUSH);
		Button btnCancel = new Button(shell, SWT.PUSH);
		
		shell.setDefaultButton(btnOk);
		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				switch (event.detail) {
					case SWT.TRAVERSE_ESCAPE:
						shell.close();
						event.detail = SWT.TRAVERSE_NONE;
						event.doit = false;
						break;
					
				}
			}
		});
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		label.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(label, SEP);
		formData.right = new FormAttachment(100, -SEP);
		text.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(text, SEP);
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(btnOk, -SEP);
		formData.width = 500;
		formData.height = 300;
		tableViewer.getTable().setLayoutData(formData);
		
		btnOk.setText("Play");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnOk.setLayoutData(formData);
		
		btnCancel.setText("Cancel");
		formData = new FormData();
		formData.right = new FormAttachment(btnOk, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnCancel.setLayoutData(formData);
		
		label.setText("Search:");
		text.addVerifyListener(textChangeListener);
		
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(shell);
		
		btnOk.addSelectionListener(buttonListener);
		btnCancel.addSelectionListener(buttonListener);
		
		shell.pack();
		
		// Work out which screen to show the dlg on.
		Point mouse = MouseInfo.getPointerInfo().getLocation();
		for (Monitor m : getParent().getDisplay().getMonitors()) {
			Rectangle b = m.getBounds();
			if (mouse.x >= b.x && mouse.x <= b.x + b.width
					&& mouse.y >= b.y && mouse.y <= b.y + b.width) {
				
				Rectangle bounds = m.getBounds ();
				Rectangle rect = shell.getBounds ();
				int x = bounds.x + (bounds.width - rect.width) / 2;
				int y = bounds.y + (bounds.height - rect.height) / 2;
				shell.setLocation (x, y);
				
				break;
			}
		}
		
		// Show the dlg.
		shell.open();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		synchronized (dlgOpen) {
			dlgOpen = false;
		}
		
		return returnValue;
	}
	
	private SelectionListener buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button b = (Button) e.widget;
			if (b.getShell().getDefaultButton() == b) {
				MediaItem item = getSelectedItem();
				if (item == null) return;
				returnValue = new PlayItem(mediaLibrary, item);
			}
			b.getShell().close();
		}
	};
	
	private MediaItem getSelectedItem () {
		ISelection selection = tableViewer.getSelection();
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object o = iSel.toList().get(0);
			if (o instanceof MediaItem) {
				MediaItem i = (MediaItem) o;
				return i;
			}
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private List<MediaItem> searchResults = null;
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		@Override
		public Object[] getElements(Object inputElement) {
			if (searchResults != null) {
				return searchResults.toArray();
				
			} else {
				return new String[]{};
			}
		}
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	};
	
	private ILabelProvider labelProvider = new LabelProvider() {
		@Override
		public String getText(Object element) {
			if (element instanceof MediaItem) {
				MediaItem item = (MediaItem) element;
				return item.toString();
			}
			return null;
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Object searchLock = new Object();
	private volatile boolean searchRunning = false;
	private volatile boolean searchDirty = false;
	
	/**
	 * Text changed event handler.
	 */
	private VerifyListener textChangeListener = new VerifyListener() {
		@Override
		public void verifyText(VerifyEvent e) {
			updateSearchResults();
		}
	};
	
	private void updateSearchResults () {
		updateSearchResults(false);
	}
	
	private void updateSearchResults (boolean force) {
		synchronized (searchLock) {
			if (!searchRunning || force) {
				getParent().getDisplay().asyncExec(updateSearchResults);
				searchRunning = true;
			} else {
				searchDirty = true;
			}
		}
	}
	
	/**
	 * Async task so we can read the complete text from the
	 * input box.
	 * Run on UI thread.
	 */
	private Runnable updateSearchResults = new Runnable() {
		@Override
		public void run() {
			if (!text.isDisposed()) {
				updateSearchResults(text.getText());
			}
		}
	};
	
	/**
	 * Run the search in a daemon thread.  Call query again if
	 * input has changed while the search was running.
	 * @param query
	 */
	private void updateSearchResults (final String query) {
		Thread t = new Thread() {
			@Override
			public void run() {
				if (doSearch(query)) {
					getParent().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (label.isDisposed() || tableViewer.getTable().isDisposed()) return;
							label.setText(searchResults.size() + " results.");
							tableViewer.refresh();
						}
					});
				}
				
				synchronized (searchLock) {
					if (searchDirty) {
						updateSearchResults(true);
						searchDirty = false;
					} else {
						searchRunning = false;
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * Do the actual searching.
	 * @param query
	 */
	private boolean doSearch (String query) {
		if (query == null || query.length() < 1) return false;
		
		String q = query.replace("'", "''");
		q = q.replace("\\", "\\\\");
		q = q.replace("%", "\\%");
		q = q.replace("_", "\\_");
		q = q.replace("*", "%");
		
		try {
			List<MediaItem> res = mediaLibrary.simpleSearch(q, "\\", 50);
			if (res != null && res.size() > 0) {
				searchResults = res;
				return true;
			}
			
		} catch (DbException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

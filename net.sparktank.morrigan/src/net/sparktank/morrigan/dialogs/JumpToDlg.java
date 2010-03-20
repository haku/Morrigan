package net.sparktank.morrigan.dialogs;

import java.awt.MouseInfo;
import java.awt.Point;
import java.lang.ref.WeakReference;
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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
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
	
	private static final int MAX_RESULTS = 50;
	
	private static volatile WeakReference<JumpToDlg> openDlg = null;
	private static volatile Boolean dlgOpen = false;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaLibrary mediaLibrary;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public JumpToDlg (Shell parent, MediaLibrary mediaLibrary) {
		super(parent, SWT.TITLE | SWT.CLOSE | SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.ON_TOP);
		
		if (mediaLibrary == null) throw new IllegalArgumentException("mediaLibrary can not be null.");
		
		this.mediaLibrary = mediaLibrary;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getKeyMask() {
		return keyMask;
	}
	
	public void setKeyMask(int keyMask) {
		this.keyMask = keyMask;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static int SEP = 3;
	
	private Shell shell;
	private Label label;
	private Text text;
	private TableViewer tableViewer;
	private Button btnPlay;
	private Button btnEnqueue;
	private Button btnCancel;
	
	private PlayItem returnValue = null;
	private int keyMask = 0;
	
	public PlayItem open () {
		synchronized (dlgOpen) {
			if (dlgOpen) {
				if (openDlg != null && openDlg.get() != null) {
					openDlg.get().remoteClose();
				}
				return null;
			}
			dlgOpen = true;
		}
		
		// Set dlg text.
		setText("Jump to track");
		
		// Create window.
		shell = new Shell(getParent().getDisplay(), getStyle());
		shell.setImage(getParent().getImage());
		shell.setText(getText());
		
		// Create form layout.
		FormData formData;
		shell.setLayout(new FormLayout());
		
		label = new Label(shell, SWT.CENTER);
		text = new Text(shell, SWT.SINGLE | SWT.CENTER | SWT.BORDER);
		tableViewer =  new TableViewer(shell, SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		btnPlay = new Button(shell, SWT.PUSH);
		btnEnqueue = new Button(shell, SWT.PUSH);
		btnCancel = new Button(shell, SWT.PUSH);
		
		shell.setDefaultButton(btnPlay);
		shell.addListener(SWT.Traverse, traverseListener);
		
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
		formData.bottom = new FormAttachment(btnPlay, -SEP);
		formData.width = 500;
		formData.height = 300;
		tableViewer.getTable().setLayoutData(formData);
		
		btnPlay.setText("Play");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnPlay.setLayoutData(formData);
		
		btnEnqueue.setText("Enqueue");
		formData = new FormData();
		formData.right = new FormAttachment(btnPlay, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnEnqueue.setLayoutData(formData);
		
		btnCancel.setText("Cancel");
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		btnCancel.setLayoutData(formData);
		
		label.setText("Search:");
		
		text.addVerifyListener(textChangeListener);
		text.addTraverseListener(textTraverseListener);
		
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(shell);
		tableViewer.getTable().addTraverseListener(listTraverseListener);
		
		btnPlay.addSelectionListener(buttonListener);
		btnEnqueue.addSelectionListener(buttonListener);
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
		
		openDlg = new WeakReference<JumpToDlg>(this);
		
		// Show the dlg.
		shell.open();
		shell.setFocus();
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void remoteClose () {
		leaveDlg(false, 0);
	}
	
	private void leaveDlg (boolean ok, int mask) {
		if (ok) {
			MediaItem item = getSelectedItem();
			if (item == null) return;
			returnValue = new PlayItem(mediaLibrary, item);
		}
		setKeyMask(mask);
		shell.close();
	}
	
	private Listener traverseListener = new Listener() {
		public void handleEvent(Event e) {
			switch (e.detail) {
				
				case SWT.TRAVERSE_RETURN:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(true, e.stateMask);
					break;
					
				case SWT.TRAVERSE_ESCAPE:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(false, e.stateMask);
					break;
				
			}
		}
	};
	
	private TraverseListener textTraverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
			switch (e.detail) {
				case SWT.TRAVERSE_ARROW_NEXT:
					if (e.keyCode == SWT.ARROW_DOWN && tableViewer.getTable().getItemCount() > 0) {
						e.detail = SWT.TRAVERSE_NONE;
						e.doit = false;
						tableViewer.getTable().setSelection(0);
						tableViewer.getTable().setFocus();
					}
			}
		}
	};
	
	private TraverseListener listTraverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
			switch (e.detail) {
				case SWT.TRAVERSE_ARROW_PREVIOUS:
					if (tableViewer.getTable().getSelectionIndex() == 0) {
						e.detail = SWT.TRAVERSE_NONE;
						e.doit = false;
						text.setFocus();
					}
			}
		}
	};
	
	private SelectionListener buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == btnPlay) {
				leaveDlg(true, 0);
				
			} else if (e.widget == btnEnqueue) {
				leaveDlg(true, SWT.CONTROL);
				
			} else {
				leaveDlg(false, 0);
				
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
							
							if (tableViewer.getTable().getItemCount() > 0) {
								tableViewer.getTable().setSelection(0);
							}
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
			List<MediaItem> res = mediaLibrary.simpleSearch(q, "\\", MAX_RESULTS);
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

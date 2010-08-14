package net.sparktank.morrigan.gui.dialogs;

import java.awt.MouseInfo;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.List;

import net.sparktank.morrigan.gui.preferences.PreferenceHelper;
import net.sparktank.morrigan.model.media.impl.MediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.morrigan.model.tracks.library.local.LocalMediaLibrary;
import net.sparktank.morrigan.model.tracks.playlist.PlayItem;
import net.sparktank.sqlitewrapper.DbException;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class JumpToDlg {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int MAX_RESULTS = 50;
	
	private static volatile WeakReference<JumpToDlg> openDlg = null;
	private static volatile Object dlgOpenLock = new Object();
	private static volatile boolean dlgOpen = false;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	final Shell parent;
	private final LocalMediaLibrary mediaLibrary;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public JumpToDlg (Shell parent, LocalMediaLibrary mediaLibrary) {
		this.parent = parent;
		if (mediaLibrary == null) throw new IllegalArgumentException("mediaLibrary can not be null.");
		this.mediaLibrary = mediaLibrary;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getKeyMask() {
		return this.keyMask;
	}
	
	public void setKeyMask(int keyMask) {
		this.keyMask = keyMask;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static int SEP = 3;
	
	private Shell shell = null;
	Label label = null;
	Text text = null;
	TableViewer tableViewer = null;
	Button btnPlay = null;
	Button btnEnqueue = null;
	private Button btnCancel = null;
	
	private PlayItem returnValue = null;
	private int keyMask = 0;
	
	public PlayItem open () {
		synchronized (dlgOpenLock) {
			if (dlgOpen) {
				if (openDlg != null) {
					JumpToDlg j = openDlg.get();
					if (j != null) {
						j.remoteClose();
					}
				}
				return null;
			}
			dlgOpen = true;
		}
		
		// Create window.
		this.shell = new Shell(this.parent.getDisplay(), SWT.TITLE | SWT.CLOSE | SWT.APPLICATION_MODAL | SWT.RESIZE | SWT.ON_TOP);
		this.shell.setImage(this.parent.getImage());
		this.shell.setText("Jump to - Morrigan");
		
		// Create form layout.
		FormData formData;
		this.shell.setLayout(new FormLayout());
		
		this.label = new Label(this.shell, SWT.CENTER);
		this.text = new Text(this.shell, SWT.SINGLE | SWT.CENTER | SWT.BORDER);
		this.tableViewer =  new TableViewer(this.shell, SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		this.btnPlay = new Button(this.shell, SWT.PUSH);
		this.btnEnqueue = new Button(this.shell, SWT.PUSH);
		this.btnCancel = new Button(this.shell, SWT.PUSH);
		
		this.shell.setDefaultButton(this.btnPlay);
		this.shell.addTraverseListener(this.traverseListener);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(0, SEP);
		formData.right = new FormAttachment(100, -SEP);
		this.label.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(this.label, SEP);
		formData.right = new FormAttachment(100, -SEP);
		this.text.setLayoutData(formData);
		
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.top = new FormAttachment(this.text, SEP);
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(this.btnPlay, -SEP);
		formData.width = 600;
		formData.height = 300;
		this.tableViewer.getTable().setLayoutData(formData);
		
		this.btnPlay.setText("Play");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnPlay.setLayoutData(formData);
		
		this.btnEnqueue.setText("Enqueue");
		formData = new FormData();
		formData.right = new FormAttachment(this.btnPlay, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnEnqueue.setLayoutData(formData);
		
		this.btnCancel.setText("Cancel");
		formData = new FormData();
		formData.left = new FormAttachment(0, SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnCancel.setLayoutData(formData);
		
		this.label.setText("Search:");
		
		this.text.addVerifyListener(this.textChangeListener);
		this.text.addTraverseListener(this.textTraverseListener);
		
		this.tableViewer.setContentProvider(this.contentProvider);
		this.tableViewer.setLabelProvider(this.labelProvider);
		this.tableViewer.setInput(this.shell);
		this.tableViewer.getTable().addTraverseListener(this.listTraverseListener);
		
		this.btnPlay.addSelectionListener(this.buttonListener);
		this.btnEnqueue.addSelectionListener(this.buttonListener);
		this.btnCancel.addSelectionListener(this.buttonListener);
		
		this.shell.pack();
		
		// Work out which screen to show the dlg on.
		Point mouse = MouseInfo.getPointerInfo().getLocation();
		for (Monitor m : this.parent.getDisplay().getMonitors()) {
			Rectangle b = m.getBounds();
			if (mouse.x >= b.x && mouse.x <= b.x + b.width
					&& mouse.y >= b.y && mouse.y <= b.y + b.width) {
				
				Rectangle bounds = m.getBounds ();
				Rectangle rect = this.shell.getBounds ();
				int x = bounds.x + (bounds.width - rect.width) / 2;
				int y = bounds.y + (bounds.height - rect.height) / 2;
				this.shell.setLocation (x, y);
				
				break;
			}
		}
		
		// Read saved query string.
		String s = PreferenceHelper.getLastJumpToDlgQuery();
		if (s != null && s.length() > 0) {
			this.text.setText(s);
			this.text.setSelection(0, this.text.getText().length());
			this.textChangeListener.verifyText(null);
		}
		
		// Save dlg object for next time.
		openDlg = new WeakReference<JumpToDlg>(this);
		
		// Show the dlg.
		this.shell.open();
		this.shell.setFocus();
		this.shell.forceActive();
		Display display = this.parent.getDisplay();
		while (!this.shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		synchronized (dlgOpenLock) {
			dlgOpen = false;
		}
		
		return this.returnValue;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void remoteClose () {
		leaveDlg(false, 0);
	}
	
	void leaveDlg (boolean ok, int mask) {
		if (ok) {
			MediaTrack item = getSelectedItem();
			if (item == null) return;
			this.returnValue = new PlayItem(this.mediaLibrary, item);
		}
		setKeyMask(mask);
		
		// Save query string.
		PreferenceHelper.setLastJumpToDlgQuery(this.text.getText());
		
		this.shell.close();
	}
	
	private TraverseListener traverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
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
				
				default:
					break;
					
			}
		}
	};
	
	private TraverseListener textTraverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
			switch (e.detail) {

				case SWT.TRAVERSE_ARROW_NEXT:
					if (e.keyCode == SWT.ARROW_DOWN && JumpToDlg.this.tableViewer.getTable().getItemCount() > 0) {
						e.detail = SWT.TRAVERSE_NONE;
						e.doit = false;
						JumpToDlg.this.tableViewer.getTable().setSelection(0);
						JumpToDlg.this.tableViewer.getTable().setFocus();
					}
					break;
				
				case SWT.TRAVERSE_ESCAPE:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(false, e.stateMask);
					break;
				
				default:
					break;
					
			}
		}
	};
	
	private TraverseListener listTraverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
			switch (e.detail) {
				
				case SWT.TRAVERSE_ARROW_PREVIOUS:
					if (JumpToDlg.this.tableViewer.getTable().getSelectionIndex() == 0) {
						e.detail = SWT.TRAVERSE_NONE;
						e.doit = false;
						JumpToDlg.this.text.setFocus();
					}
					break;
				
				case SWT.TRAVERSE_ESCAPE:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(false, e.stateMask);
					break;
				
				default:
					break;
				
			}
		}
	};
	
	private SelectionListener buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == JumpToDlg.this.btnPlay) {
				leaveDlg(true, 0);
				
			} else if (e.widget == JumpToDlg.this.btnEnqueue) {
				leaveDlg(true, SWT.CONTROL);
				
			} else {
				leaveDlg(false, 0);
				
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaTrack getSelectedItem () {
		ISelection selection = this.tableViewer.getSelection();
		if (selection==null) return null;
		if (selection.isEmpty()) return null;
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object o = iSel.toList().get(0);
			if (o instanceof MediaItem) {
				MediaTrack i = (MediaTrack) o;
				return i;
			}
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	List<IMediaTrack> searchResults = null;
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		@Override
		public Object[] getElements(Object inputElement) {
			if (JumpToDlg.this.searchResults != null) {
				return JumpToDlg.this.searchResults.toArray();
				
			}
			
			return new String[]{};
		}
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {/* UNUSED */}
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
	
	Object searchLock = new Object();
	volatile boolean searchRunning = false;
	volatile boolean searchDirty = false;
	
	/**
	 * Text changed event handler.
	 */
	private VerifyListener textChangeListener = new VerifyListener() {
		@Override
		public void verifyText(VerifyEvent e) {
			updateSearchResults();
		}
	};
	
	void updateSearchResults () {
		updateSearchResults(false);
	}
	
	void updateSearchResults (boolean force) {
		synchronized (this.searchLock) {
			if (!this.searchRunning || force) {
				this.parent.getDisplay().asyncExec(this.updateSearchResults);
				this.searchRunning = true;
			} else {
				this.searchDirty = true;
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
			if (!JumpToDlg.this.text.isDisposed()) {
				updateSearchResults(JumpToDlg.this.text.getText());
			}
		}
	};
	
	/**
	 * Run the search in a daemon thread.  Call query again if
	 * input has changed while the search was running.
	 * @param query
	 */
	void updateSearchResults (final String query) {
		Thread t = new Thread() {
			@Override
			public void run() {
				if (doSearch(query)) {
					JumpToDlg.this.parent.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (JumpToDlg.this.label.isDisposed() || JumpToDlg.this.tableViewer.getTable().isDisposed()) return;
							JumpToDlg.this.label.setText(JumpToDlg.this.searchResults.size() + " results.");
							JumpToDlg.this.tableViewer.refresh();
							
							if (JumpToDlg.this.tableViewer.getTable().getItemCount() > 0) {
								JumpToDlg.this.tableViewer.getTable().setSelection(0);
							}
						}
					});
				
				} else {
					JumpToDlg.this.parent.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (JumpToDlg.this.text.getText().length() > 0) {
								JumpToDlg.this.label.setText("No results for query.");
							} else {
								JumpToDlg.this.label.setText("Search:");
							}
						}
					});
				}
				
				synchronized (JumpToDlg.this.searchLock) {
					if (JumpToDlg.this.searchDirty) {
						updateSearchResults(true);
						JumpToDlg.this.searchDirty = false;
					} else {
						JumpToDlg.this.searchRunning = false;
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
	boolean doSearch (String query) {
		if (query == null || query.length() < 1) return false;
		
		String q = query.replace("'", "''");
		q = q.replace(" ", "*");
		q = q.replace("\\", "\\\\");
		q = q.replace("%", "\\%");
		q = q.replace("_", "\\_");
		q = q.replace("*", "%");
		
		try {
			List<IMediaTrack> res = this.mediaLibrary.simpleSearch(q, "\\", MAX_RESULTS);
			if (res != null && res.size() > 0) {
				this.searchResults = res;
				return true;
			}
			
		} catch (DbException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

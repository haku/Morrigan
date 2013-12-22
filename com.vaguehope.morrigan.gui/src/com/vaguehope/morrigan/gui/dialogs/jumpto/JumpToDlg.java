package com.vaguehope.morrigan.gui.dialogs.jumpto;

import java.lang.ref.WeakReference;
import java.util.List;

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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.vaguehope.morrigan.gui.preferences.PreferenceHelper;
import com.vaguehope.morrigan.gui.util.MonitorHelper;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackDb;
import com.vaguehope.sqlitewrapper.DbException;

public class JumpToDlg {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int MAX_RESULTS = 200;

	private static volatile WeakReference<JumpToDlg> openDlg = null;
	private static volatile Object dlgOpenLock = new Object();
	private static volatile boolean dlgOpen = false;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final Shell parent;
	private final IMediaTrackDb<?,? extends IMediaTrack> mediaDb;

	private IMediaTrack returnItem = null;
	private String returnFilter = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public JumpToDlg (final Shell parent, final IMediaTrackDb<?,? extends IMediaTrack> mediaDb) {
		this.parent = parent;
		if (mediaDb == null) throw new IllegalArgumentException("mediaDb can not be null.");
		this.mediaDb = mediaDb;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public IMediaTrack getReturnItem () {
		return this.returnItem;
	}

	public List<? extends IMediaTrack> getSearchResults () {
		return this.searchResults;
	}

	public String getReturnFilter() {
		return this.returnFilter;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int SEP = 3;

	private Shell shell = null;
	Label label = null;
	Text text = null;
	TableViewer tableViewer = null;
	Button btnPlay = null;
	Button btnEnqueue = null;
	Button btnReveal = null;
	Button btnShuffleAll = null;
	Button btnOpenView = null;
	private Button btnCancel = null;

	private JumpType resultAction;

	public JumpType open () {
		synchronized (dlgOpenLock) {
			if (dlgOpen) {
				if (openDlg != null) {
					JumpToDlg j = openDlg.get();
					if (j != null) {
						j.remoteClose();
					}
				}
				return JumpType.NULL;
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
		this.btnReveal = new Button(this.shell, SWT.PUSH);
		this.btnShuffleAll = new Button(this.shell, SWT.PUSH);
		this.btnOpenView = new Button(this.shell, SWT.PUSH);
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

		this.btnOpenView.setText("Open view");
		formData = new FormData();
		formData.right = new FormAttachment(100, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnOpenView.setLayoutData(formData);

		this.btnShuffleAll.setText("Shuffle and enqueue");
		formData = new FormData();
		formData.right = new FormAttachment(this.btnOpenView, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnShuffleAll.setLayoutData(formData);

		this.btnPlay.setText("Play");
		formData = new FormData();
		formData.right = new FormAttachment(this.btnShuffleAll, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnPlay.setLayoutData(formData);

		this.btnEnqueue.setText("Enqueue");
		formData = new FormData();
		formData.right = new FormAttachment(this.btnPlay, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnEnqueue.setLayoutData(formData);

		this.btnReveal.setText("Reveal");
		formData = new FormData();
		formData.right = new FormAttachment(this.btnEnqueue, -SEP);
		formData.bottom = new FormAttachment(100, -SEP);
		this.btnReveal.setLayoutData(formData);

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
		this.btnReveal.addSelectionListener(this.buttonListener);
		this.btnShuffleAll.addSelectionListener(this.buttonListener);
		this.btnOpenView.addSelectionListener(this.buttonListener);
		this.btnCancel.addSelectionListener(this.buttonListener);

		this.shell.pack();
		MonitorHelper.moveShellToActiveMonitor(this.shell);

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

		return this.resultAction;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void remoteClose () {
		leaveDlg(JumpType.NULL);
	}

	void leaveDlg (final JumpType action) {
		this.returnItem = getSelectedItem();
		this.returnFilter = this.text.getText();

		if (this.returnItem == null) {
			this.resultAction = JumpType.NULL;
		}
		else if ((action == JumpType.OPEN_VIEW || action == JumpType.SHUFFLE_AND_ENQUEUE) && this.searchResults == null) {
			this.resultAction = JumpType.NULL;
		}
		else {
			this.resultAction = action;
		}

		// Save query string.
		PreferenceHelper.setLastJumpToDlgQuery(this.text.getText());

		this.shell.close();
	}

	private final TraverseListener traverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(final TraverseEvent e) {
			switch (e.detail) {

				case SWT.TRAVERSE_RETURN:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(JumpType.fromStateMask(e.stateMask));
					break;

				case SWT.TRAVERSE_ESCAPE:
					e.detail = SWT.TRAVERSE_NONE;
					e.doit = false;
					leaveDlg(JumpType.NULL);
					break;

				default:
					break;

			}
		}
	};

	private final TraverseListener textTraverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(final TraverseEvent e) {
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
					leaveDlg(JumpType.NULL);
					break;

				default:
					break;

			}
		}
	};

	private final TraverseListener listTraverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(final TraverseEvent e) {
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
					leaveDlg(JumpType.NULL);
					break;

				default:
					break;

			}
		}
	};

	private final SelectionListener buttonListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(final SelectionEvent e) {
			if (e.widget == JumpToDlg.this.btnPlay) {
				leaveDlg(JumpType.PLAY_NOW);
			}
			else if (e.widget == JumpToDlg.this.btnEnqueue) {
				leaveDlg(JumpType.ENQUEUE);
			}
			else if (e.widget == JumpToDlg.this.btnReveal) {
				leaveDlg(JumpType.REVEAL);
			}
			else if (e.widget == JumpToDlg.this.btnShuffleAll) {
				leaveDlg(JumpType.SHUFFLE_AND_ENQUEUE);
			}
			else if (e.widget == JumpToDlg.this.btnOpenView) {
				leaveDlg(JumpType.OPEN_VIEW);
			}
			else {
				leaveDlg(JumpType.NULL);
			}
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private IMediaTrack getSelectedItem () {
		ISelection selection = this.tableViewer.getSelection();
		if (selection==null) return null;
		if (selection.isEmpty()) return null;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iSel = (IStructuredSelection) selection;
			Object o = iSel.toList().get(0);
			if (o instanceof IMediaTrack) {
				IMediaTrack i = (IMediaTrack) o;
				return i;
			}
		}

		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	volatile List<? extends IMediaTrack> searchResults = null;

	private final IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		@Override
		public Object[] getElements(final Object inputElement) {
			if (JumpToDlg.this.searchResults != null) {
				return JumpToDlg.this.searchResults.toArray();
			}
			return new String[]{};
		}
		@Override
		public void dispose() {/* UNUSED */}
		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {/* UNUSED */}
	};

	private final ILabelProvider labelProvider = new LabelProvider() {
		@Override
		public String getText(final Object element) {
			if (element  != null) {
				return element.toString();
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
	private final VerifyListener textChangeListener = new VerifyListener() {
		@Override
		public void verifyText(final VerifyEvent e) {
			updateSearchResults();
		}
	};

	void updateSearchResults () {
		updateSearchResults(false);
	}

	void updateSearchResults (final boolean force) {
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
	private final Runnable updateSearchResults = new Runnable() {
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
	boolean doSearch (final String query) {
		if (query == null || query.length() < 1) return false;

		try {
			List<? extends IMediaTrack> res = this.mediaDb.simpleSearch(query, MAX_RESULTS);
			if (res != null && res.size() > 0) {
				this.searchResults = res;
				return true;
			}
		}
		catch (DbException e) {
			e.printStackTrace();
		}

		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

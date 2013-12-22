package com.vaguehope.morrigan.gui.dialogs.jumpto;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import com.vaguehope.morrigan.gui.dialogs.Dismissable;
import com.vaguehope.morrigan.gui.dialogs.RunnableDialog;
import com.vaguehope.morrigan.gui.helpers.MonitorHelper;
import com.vaguehope.morrigan.gui.helpers.UiThreadHelper;
import com.vaguehope.morrigan.gui.preferences.PreferenceHelper;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackDb;
import com.vaguehope.sqlitewrapper.DbException;

public class JumpToDlg implements Dismissable {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int MAX_RESULTS = 200;

	private static volatile WeakReference<JumpToDlg> openDlg = null;
	private static volatile Object dlgOpenLock = new Object();
	private static volatile boolean dlgOpen = false;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Shell parent;
	private final IMediaTrackDb<?, ? extends IMediaTrack> mediaDb;

	private JumpType resultAction;
	private IMediaTrack returnItem = null;
	private String returnFilter = null;
	private List<? extends IMediaTrack> searchResults = null;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public JumpToDlg (final Shell parent, final IMediaTrackDb<?, ? extends IMediaTrack> mediaDb) {
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

	public String getReturnFilter () {
		return this.returnFilter;
	}

	protected Shell getParent () {
		return this.parent;
	}

	protected IMediaTrackDb<?, ? extends IMediaTrack> getMediaDb () {
		return this.mediaDb;
	}

	protected boolean isAlive () {
		return !this.shell.isDisposed();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final int SEP = 3;

	private Shell shell = null;
	private Label label = null;
	private Text text = null;
	private TableViewer tableViewer = null;
	private Button btnPlay = null;
	private Button btnEnqueue = null;
	private Button btnReveal = null;
	private Button btnShuffleAll = null;
	private Button btnOpenView = null;
	private Button btnCancel = null;

	public JumpType open () {
		synchronized (dlgOpenLock) {
			if (dlgOpen) {
				if (openDlg != null) {
					final JumpToDlg j = openDlg.get();
					if (j != null) {
						j.dismiss();
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
		this.tableViewer = new TableViewer(this.shell, SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
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
		this.text.addTraverseListener(new TextWithTableBelowTraverseListener(this.tableViewer, this));

		this.tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		this.tableViewer.getTable().addTraverseListener(new TableWithTextBoxAboveTraverseListener(this.tableViewer, this.text, this));

		this.btnPlay.addSelectionListener(new JumpSelectionAdaptor(this, JumpType.PLAY_NOW));
		this.btnEnqueue.addSelectionListener(new JumpSelectionAdaptor(this, JumpType.ENQUEUE));
		this.btnReveal.addSelectionListener(new JumpSelectionAdaptor(this, JumpType.REVEAL));
		this.btnShuffleAll.addSelectionListener(new JumpSelectionAdaptor(this, JumpType.SHUFFLE_AND_ENQUEUE));
		this.btnOpenView.addSelectionListener(new JumpSelectionAdaptor(this, JumpType.OPEN_VIEW));
		this.btnCancel.addSelectionListener(new JumpSelectionAdaptor(this, JumpType.NULL));

		this.shell.pack();
		MonitorHelper.moveShellToActiveMonitor(this.shell);

		// Read saved query string.
		final String s = PreferenceHelper.getLastJumpToDlgQuery();
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
		final Display display = this.parent.getDisplay();
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

	@Override
	public void dismiss () {
		leaveDlg(JumpType.NULL);
	}

	protected void leaveDlg (final JumpType action) {
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
		public void keyTraversed (final TraverseEvent e) {
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

	private static class JumpSelectionAdaptor extends SelectionAdapter {

		private final JumpToDlg dlg;
		private final JumpType jumpType;

		public JumpSelectionAdaptor (final JumpToDlg dlg, final JumpType jumpType) {
			this.dlg = dlg;
			this.jumpType = jumpType;
		}

		@Override
		public void widgetSelected (final SelectionEvent e) {
			this.dlg.leaveDlg(this.jumpType);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected String getSearchText () {
		if (this.text.isDisposed()) return null;
		return this.text.getText();
	}

	private IMediaTrack getSelectedItem () {
		final ISelection selection = this.tableViewer.getSelection();
		if (selection == null) return null;
		if (selection.isEmpty()) return null;
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection iSel = (IStructuredSelection) selection;
			final Object o = iSel.toList().get(0);
			if (o instanceof IMediaTrack) {
				final IMediaTrack i = (IMediaTrack) o;
				return i;
			}
		}
		return null;
	}

	/**
	 * Call on UI thread.
	 */
	protected void setSearchResults (final List<? extends IMediaTrack> results) {
		this.searchResults = results;
		if (this.label.isDisposed() || this.tableViewer.getTable().isDisposed()) return;
		if (results != null && results.size() > 0) {
			this.label.setText(results.size() + " results.");
			this.tableViewer.setInput(results);
			if (this.tableViewer.getTable().getItemCount() > 0) this.tableViewer.getTable().setSelection(0);
		}
		else if (this.text.getCharCount() > 0) {
			this.label.setText("No results for query.");
		}
		else {
			this.label.setText("Search:");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final VerifyListener textChangeListener = new VerifyListener() {
		@Override
		public void verifyText (final VerifyEvent e) {
			if (e == null || e.text == null || e.text.length() < 1) return;
			requestSearch();
		}
	};

	private final Object searchLock = new Object();
	private SearchRunner searchRunner;

	/**
	 * Only call on UI thread.
	 */
	protected void requestSearch () {
		synchronized (this.searchLock) {
			if (this.searchRunner == null) {
				this.searchRunner = new SearchRunner(this);
				final Thread t = new Thread(this.searchRunner);
				t.setDaemon(true);
				t.start();
			}
			this.searchRunner.request();
		}
	}

	private static class SearchRunner implements Runnable {

		private final JumpToDlg dlg;
		private final BlockingQueue<Object> queue;

		public SearchRunner (final JumpToDlg dlg) {
			this.dlg = dlg;
			this.queue = new LinkedBlockingQueue<Object>(1);
		}

		public void request() {
			this.queue.offer(Boolean.TRUE);
		}

		@Override
		public void run () {
			try {
				runAndThrow();
			}
			catch (final DbException e) {
				this.dlg.getParent().getDisplay().asyncExec(new RunnableDialog(e));
			}
//			System.err.println("Thread " + Thread.currentThread().getId() + " over.");
		}

		private void runAndThrow () throws DbException {
			while (this.dlg.isAlive()) {
				try {
					if (this.queue.poll(15, TimeUnit.SECONDS) != null) {
						final List<? extends IMediaTrack> results = doSearch(this.dlg);
						this.dlg.getParent().getDisplay().syncExec(new SetSearchResults(this.dlg, results));
					}
				}
				catch (InterruptedException e) { /* ignore. */}
			}
		}

		protected static List<? extends IMediaTrack> doSearch (final JumpToDlg dlg) throws DbException {
			final String query = UiThreadHelper.callForResult(dlg.getParent().getDisplay(), new Callable<String>() {
				@Override
				public String call () {
					return dlg.getSearchText();
				}
			});
			if (query == null || query.length() < 1) return null;
//			System.err.println("t=" + Thread.currentThread().getId() + " q=" + query);
			return dlg.getMediaDb().simpleSearch(query, MAX_RESULTS);
		}

	}

	private static class SetSearchResults implements Runnable {

		private final JumpToDlg dlg;
		private final List<? extends IMediaTrack> results;

		public SetSearchResults (final JumpToDlg dlg, final List<? extends IMediaTrack> results) {
			this.dlg = dlg;
			this.results = results;
		}

		@Override
		public void run () {
			this.dlg.setSearchResults(this.results);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

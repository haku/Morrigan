package com.vaguehope.morrigan.sshui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MatchMode;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.Autocomplete.MergedResult;
import com.vaguehope.morrigan.sshui.Autocomplete.PartialQuery;
import com.vaguehope.morrigan.util.StringHelper;

public class JumpToDialog extends DialogWindow {

	private static final int WIDTH = 70;
	private static final int HEIGHT = 14;
	private static final int MAX_RESULTS = 100;
	private static final int MAX_AUTOCOMPLETE_RESULTS = 20;
	private static final Logger LOG = LoggerFactory.getLogger(JumpToDialog.class);

	private final IMixedMediaDb db;
	private final AtomicReference<String> savedSearchTerm;

	private final Label lblMsgs;
	private final SearchTextBox txtSearch;
	private final MediaItemListBox lstResults;
	private final Label lblTags;

	private volatile boolean alive = true;
	private List<? extends IMediaTrack> searchResults;
	private JumpResult result;

	public JumpToDialog (final IMixedMediaDb db, final AtomicReference<String> savedSearchTerm) {
		super(db.getListName());
		this.db = db;
		this.savedSearchTerm = savedSearchTerm;

		final Panel p = new Panel();
		p.setLayoutManager(new GridLayout(1)
				.setLeftMarginSize(1)
				.setRightMarginSize(1));

		this.lblMsgs = new Label("");
		p.addComponent(this.lblMsgs);

		this.txtSearch = new SearchTextBox(new TerminalSize(WIDTH, 1), this);
		p.addComponent(this.txtSearch);

		this.lstResults = new MediaItemListBox(new TerminalSize(WIDTH, HEIGHT), this);
		p.addComponent(this.lstResults);

		this.lblTags = new FixedHeightLabel("", 2);
		this.lblTags.setLabelWidth(WIDTH);
		p.addComponent(this.lblTags);

		final Panel btnPanel = new Panel();
		btnPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));
		btnPanel.addComponent(new Button("Reveal", new Runnable() {
			@Override
			public void run () {
				acceptRevealResult();
			}
		}));
		btnPanel.addComponent(new Button("Enqueue All", new Runnable() {
			@Override
			public void run () {
				acceptEnqueueAllResult();
			}
		}));
		btnPanel.addComponent(new Button("Shuffle", new Runnable() {
			@Override
			public void run () {
				acceptShuffleResult();
			}
		}));
		btnPanel.addComponent(new EmptySpace(new TerminalSize(WIDTH - 58, 1))); // FIXME magic numbers.
		btnPanel.addComponent(new Button("Open View", new Runnable() {
			@Override
			public void run () {
				acceptOpenResult();
			}
		}));
		btnPanel.addComponent(new Button("Close", new Runnable() {
			@Override
			public void run () {
				close();
			}
		}));

		btnPanel.addTo(p);
		setComponent(p);

		setCloseWindowWithEscape(true);
		setHints(new HashSet<>(Arrays.asList(Hint.CENTERED, Hint.MODAL)));

		setSearchResults(null); // Init msgs.
		final String term = savedSearchTerm.get();
		if (term != null) {
			this.txtSearch.setText(term);
			requestSearch(new PartialQuery(term, null, -1));
		}
	}

	@Override
	public void close () {
		this.alive = false;
		super.close();
	}

	protected boolean isAlive () {
		return this.alive && getTextGUI() != null;
	}

	private static final AtomicInteger BG_THREAD_NUMBER = new AtomicInteger(0);
	private SearchRunner searchRunner;

	/**
	 * Only call on UI thread.
	 */
	protected final void requestSearch (final PartialQuery partialQuery) {
		if (this.searchRunner == null) {
			this.searchRunner = new SearchRunner(this);
			final Thread t = new Thread(this.searchRunner, String.format("jtbg-%s", BG_THREAD_NUMBER.getAndIncrement()));
			t.setDaemon(true);
			t.start();
		}
		this.searchRunner.requestSearch(partialQuery);
	}

	/**
	 * Only call on UI thread.
	 */
	protected void requestTags (final IMediaTrack item) {
		if (this.searchRunner == null) return;
		this.searchRunner.requestTags(item);
	}

	private static class SearchRunner implements Runnable {

		private final JumpToDialog dlg;
		private final BlockingQueue<Object> queue;

		public SearchRunner (final JumpToDialog dlg) {
			this.dlg = dlg;
			this.queue = new LinkedBlockingQueue<>(1);
		}

		public void requestSearch (final PartialQuery partialQuery) {
			this.queue.offer(partialQuery);
		}

		public void requestTags (final IMediaTrack item) {
			if (item == null) return;
			this.queue.offer(item);
		}

		@Override
		public void run () {
			try {
				runAndThrow();
			}
			catch (final Exception e) { // NOSONAR report all errors to user.
				this.dlg.getTextGUI().getGUIThread().invokeLater(new Runnable() {
					@Override
					public void run () {
						MessageDialog.showMessageDialog(SearchRunner.this.dlg.getTextGUI(), "Error running search", e.toString());
					}
				});
			}
			LOG.info("BG search thread done.");
		}

		private void runAndThrow () throws DbException, MorriganException {
			while (this.dlg.isAlive()) {
				try {
					final Object item = this.queue.poll(15, TimeUnit.SECONDS);
					if (item == null) continue;
					if (item instanceof PartialQuery) {
						final PartialQuery partialQuery = (PartialQuery) item;

						if (partialQuery.activeTerm != null) {
							String tagQuery = partialQuery.activeTerm;

							MatchMode matchMode = MatchMode.PREFIX;
							if (tagQuery.startsWith("t=")) {
								tagQuery = tagQuery.substring(2);
							}
							else if (tagQuery.startsWith("t~")) {
								tagQuery = tagQuery.substring(2);
								matchMode = MatchMode.SUBSTRING;
							}

							final Map<String, MediaTag> tags = this.dlg.db.tagSearch(tagQuery, matchMode, MAX_AUTOCOMPLETE_RESULTS);
							this.dlg.getTextGUI().getGUIThread().invokeLater(new ShowAutocomplete(this.dlg, partialQuery, tags));
						}

						final List<? extends IMediaTrack> results = this.dlg.db.simpleSearch(partialQuery.fullText, MAX_RESULTS);
						this.dlg.getTextGUI().getGUIThread().invokeLater(new SetSearchResults(this.dlg, results));
					}
					else if (item instanceof IMediaTrack) {
						final List<MediaTag> tags = this.dlg.db.getTags((IMediaTrack) item);
						this.dlg.getTextGUI().getGUIThread().invokeLater(new ShowTags(this.dlg, tags));
					}
					else {
						LOG.warn("Unknown object on queue: {}", item);
					}
				}
				catch (final InterruptedException e) { /* ignore. */}
			}
		}

	}

	private static class ShowAutocomplete implements Runnable {

		private final JumpToDialog dlg;
		private final PartialQuery partialQuery;
		private final Map<String, MediaTag> tags;

		public ShowAutocomplete(final JumpToDialog dlg, final PartialQuery partialQuery, final Map<String, MediaTag> tags) {
			this.dlg = dlg;
			this.partialQuery = partialQuery;
			this.tags = tags;
		}

		@Override
		public void run() {
			this.dlg.showAutocomplete(this.partialQuery, this.tags);
		}

	}

	private static class SetSearchResults implements Runnable {

		private final JumpToDialog dlg;
		private final List<? extends IMediaTrack> results;

		public SetSearchResults (final JumpToDialog dlg, final List<? extends IMediaTrack> results) {
			this.dlg = dlg;
			this.results = results;
		}

		@Override
		public void run () {
			this.dlg.setSearchResults(this.results);
		}
	}

	private static class ShowTags implements Runnable {

		private final JumpToDialog dlg;
		private final List<MediaTag> tags;

		public ShowTags (final JumpToDialog dlg, final List<MediaTag> tags) {
			this.dlg = dlg;
			this.tags = tags;
		}

		@Override
		public void run () {
			final StringBuilder s = new StringBuilder();
			for (final MediaTag tag : this.tags) {
				if (tag.getType() != MediaTagType.MANUAL) continue;
				if (s.length() > 0) s.append(", ");
				s.append(tag.getTag());
			}
			this.dlg.showTags(s.toString());
		}
	}

	protected String getSearchText () {
		final String text = this.txtSearch.getText();
		this.savedSearchTerm.set(text);
		return text;
	}

	/**
	 * Call on UI thread.
	 */
	protected final void showAutocomplete (final PartialQuery partialQuery, final Map<String, MediaTag> tags) {
		final List<Runnable> actions = new ArrayList<>();
		for (final Entry<String, MediaTag> e : tags.entrySet()) {
			actions.add(new Runnable() {
				@Override
				public void run() {
					final MergedResult mr = Autocomplete.mergeResult(JumpToDialog.this.txtSearch.getText(), partialQuery, e.getValue().getTag());
					if (mr == null) return;
					JumpToDialog.this.txtSearch.setText(mr.result);
					JumpToDialog.this.txtSearch.setCaretPosition(mr.caretPos);
					requestSearch(new PartialQuery(mr.result, null, -1));
				}

				@Override
				public String toString() {
					return e.getKey();
				}
			});
		}
		this.txtSearch.showAutocomplete(actions);
	}

	/**
	 * Call on UI thread.
	 */
	protected final void setSearchResults (final List<? extends IMediaTrack> results) {
		this.searchResults = results;
		this.lstResults.setItems(results);
		if (results != null && results.size() > 0) {
			this.lblMsgs.setText(results.size() + " results.");
		}
		else if (this.txtSearch.getText().length() > 0) {
			this.lblMsgs.setText("No results for query.");
		}
		else {
			this.lblMsgs.setText("Search:");
		}
	}

	protected void showTags (final String msg) {
		this.lblTags.setText(msg);
	}

	protected void acceptEnqueueResult () {
		final IMediaTrack track = this.lstResults.getSelectedTrack();
		if (track != null) setResult(new JumpResult(JumpType.ENQUEUE, this.txtSearch.getText(), track));
	}

	protected void acceptEnqueueAllResult () {
		if (this.searchResults != null && this.searchResults.size() > 0) {
			setResult(new JumpResult(JumpType.ENQUEUE, this.txtSearch.getText(), this.searchResults));
		}
	}

	protected void acceptOpenResult () {
		final String text = this.txtSearch.getText();
		if (text != null && text.length() > 0) {
			setResult(new JumpResult(JumpType.OPEN_VIEW, text));
			return;
		}

		final String dbSearch = this.db.getSearchTerm();
		if (StringHelper.notBlank(dbSearch)) {
			setResult(new JumpResult(JumpType.OPEN_VIEW, dbSearch));
		}
	}

	protected void acceptRevealResult () {
		final IMediaTrack track = this.lstResults.getSelectedTrack();
		if (track != null) setResult(new JumpResult(JumpType.REVEAL, this.txtSearch.getText(), track));
	}

	protected void acceptShuffleResult () {
		if (this.searchResults != null && this.searchResults.size() > 0) {
			setResult(new JumpResult(JumpType.SHUFFLE_AND_ENQUEUE, this.txtSearch.getText(), this.searchResults));
		}
	}

	private void setResult (final JumpResult res) {
		if (this.result != null) throw new IllegalStateException();
		this.result = res;
		close();
	}

	public JumpResult getResult () {
		return this.result;
	}

	private static class SearchTextBox extends TextBox {

		private final JumpToDialog dialog;
		private AutocompletePopup autocompletePopup;

		public SearchTextBox (final TerminalSize preferredSize, final JumpToDialog dialog) {
			super(preferredSize);
			this.dialog = dialog;
			setTextChangeListener((newText, changedByUserInteraction) -> {
				if (!changedByUserInteraction) return;
				onTextChangedByUser(newText);
			});
		}

		private void onTextChangedByUser(final String newText) {
			final PartialQuery partialQuery = Autocomplete.extractPartialSearchTerm(newText, getCaretPosition().getColumn());
			if (partialQuery.activeTerm == null) closeAutocomplete();
			this.dialog.requestSearch(partialQuery);
		}

		@Override
		public synchronized Result handleKeyStroke (final KeyStroke key) {
			if (this.autocompletePopup != null && this.autocompletePopup.offerInput(key)) {
				return Result.HANDLED;
			}

			switch (key.getKeyType()) {
			case Enter:
				this.dialog.acceptEnqueueResult();
				return Result.HANDLED;
			case Character:
				if (key.isCtrlDown() && key.getCharacter() == 'u') {
					setText("");
					return Result.HANDLED;
				}
				//$FALL-THROUGH$
			default:
				return super.handleKeyStroke(key);
			}
		}

		@Override
		protected synchronized void afterLeaveFocus(final FocusChangeDirection direction, final Interactable nextInFocus) {
			closeAutocomplete();
		}

		public void showAutocomplete(final List<Runnable> actions) {
			closeAutocomplete();
			if (actions.size() < 1) return;
			this.autocompletePopup = AutocompletePopup.makeAndShow(this, actions, () -> this.autocompletePopup = null);
		}

		private void closeAutocomplete() {
			if (this.autocompletePopup == null) return;
			this.autocompletePopup.close();
			this.autocompletePopup = null;
		}

	}

	private static class MediaItemListBox extends AbstractListBox<IMediaTrack, MediaItemListBox> {

		private final JumpToDialog dialog;

		public MediaItemListBox (final TerminalSize preferredSize, final JumpToDialog dialog) {
			super(preferredSize);
			this.dialog = dialog;
		}

		public void setItems (final List<? extends IMediaTrack> items) {
			clearItems();
			if (items != null) {
				for (final IMediaTrack track : items) {
					addItem(track);
				}
				setSelectedIndex(0);
			}
		}

		public IMediaTrack getSelectedTrack () {
			if (getItemCount() < 1) return null;
			return getSelectedItem();
		}

		@Override
		protected ListItemRenderer<IMediaTrack, MediaItemListBox> createDefaultListItemRenderer() {
			return new ListItemRenderer<IMediaTrack, MediaItemListBox>() {
				@Override
				public int getHotSpotPositionOnLine(final int selectedIndex) {
					return -1;
				}
				@Override
				public void drawItem(final TextGUIGraphics graphics, final MediaItemListBox listBox, final int index, final IMediaTrack item, final boolean selected, final boolean focused) {
					super.drawItem(graphics, listBox, index, item, selected, true);
				}
			};
		}

		@Override
		protected synchronized void afterEnterFocus (final FocusChangeDirection direction, final Interactable previouslyInFocus) {
			super.afterEnterFocus(direction, previouslyInFocus);
			selectedChanged();
		}

		@Override
		public synchronized Result handleKeyStroke(final KeyStroke key) {
			final Result result = super.handleKeyStroke(key);

			switch (key.getKeyType()) {
				case ArrowUp:
				case ArrowDown:
					selectedChanged();
				//$FALL-THROUGH$
			default:
			}

			if (result == Result.UNHANDLED) {
				switch (key.getKeyType()) {
					case Enter:
						this.dialog.acceptEnqueueResult();
						return Result.HANDLED;
					default:
						return Result.UNHANDLED;
				}
			}

			return result;
		}

		private void selectedChanged () {
			this.dialog.requestTags(getSelectedItem());
		}

	}

	private static class FixedHeightLabel extends Label {
		private final int height;

		public FixedHeightLabel(final String text, final int height) {
			super(text);
			this.height = height;
			setText(text);  // Force getBounds() to be called AFTER setting height field.
		}

		@Override
		protected TerminalSize getBounds(String[] lines, TerminalSize currentBounds) {
			return super.getBounds(lines, currentBounds).withRows(this.height);
		}
	}

	public enum JumpType {
		ENQUEUE,
		REVEAL,
		SHUFFLE_AND_ENQUEUE,
		OPEN_VIEW;
	}

	public static class JumpResult {

		private final JumpType type;
		private final String text;
		private final IMediaTrack track;
		private final List<? extends IMediaTrack> tracks;

		public JumpResult (final JumpType type, final String text) {
			this(type, text, null, null);
		}

		public JumpResult (final JumpType type, final String text, final IMediaTrack track) {
			this(type, text, track, null);
		}

		public JumpResult (final JumpType type, final String text, final List<? extends IMediaTrack> tracks) {
			this(type, text, null, tracks);
		}

		private JumpResult (final JumpType type, final String text, final IMediaTrack track, final List<? extends IMediaTrack> tracks) {
			if (type == null) throw new IllegalArgumentException("type not specified");
			this.type = type;
			this.track = track;
			this.text = text;
			this.tracks = tracks;
		}

		public JumpType getType () {
			return this.type;
		}

		public String getText () {
			if (this.text == null) throw new IllegalStateException("text not set.");
			return this.text;
		}

		public IMediaTrack getTrack () {
			if (this.track == null) throw new IllegalStateException("track not set.");
			return this.track;
		}

		public List<? extends IMediaTrack> getTracks () {
			if (this.tracks == null) {
				if (this.track != null) return Collections.singletonList(this.track);
				throw new IllegalStateException("tracks not set.");
			}
			return this.tracks;
		}

	}

	public static JumpResult show (final WindowBasedTextGUI owner, final IMixedMediaDb db, final AtomicReference<String> savedSearchTerm) {
		final JumpToDialog dialog = new JumpToDialog(db, savedSearchTerm);
		dialog.showDialog(owner);
		return dialog.getResult();
	}

}

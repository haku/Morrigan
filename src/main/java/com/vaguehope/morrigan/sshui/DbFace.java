package com.vaguehope.morrigan.sshui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialog;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;
import com.vaguehope.morrigan.sshui.util.LastActionMessage;
import com.vaguehope.morrigan.sshui.util.TextGuiUtils;
import com.vaguehope.morrigan.tasks.MorriganTask;

public class DbFace extends DefaultFace {

	private static final String HELP_TEXT =
			"      g\tgo to top of list\n" +
			"      G\tgo to end of list\n" +
			"      z\tcentre selection\n" +
			"      /\tsearch DB\n" +
			"      o\tsort order\n" +
			"      e\tenqueue item(s)\n" +
			"      E\tenqueue DB\n" +
			"<enter>\tplay item(s)\n" +
			"      t\ttag editor\n" +
			"      v\tselect\n" +
			"      x\tselect\n" +
			"      X\tempty selection\n" +
			"      w\tcopy file(s)\n" +
			"      d\ttoggle item(s) enabled\n" +
			"      r\trefresh query\n" +
			"      p\tDB properties\n" +
			"      q\tback a page\n" +
			"      h\tthis help text";

	private static final String PREF_SCROLL_INDEX = "dbscroll";
	private static final String PREF_SELECTED_INDEX = "dbselected";

	private final FaceNavigation navigation;
	private final MnContext mnContext;
	private final SessionState sessionState;
	private final IMediaItemList list;
	private final String nodeId;
	private final Player defaultPlayer;
	private final DbHelper dbHelper;

	private final TextGuiUtils textGuiUtils = new TextGuiUtils();
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private final LastActionMessage lastActionMessage = new LastActionMessage();
	private final Set<IMediaItem> selectedItems = new HashSet<>();

	private MediaNode mediaNode;
	private int selectedItemIndex = -1;
	private int scrollTop = 0;
	private int pageSize = 1;
	private VDirection lastMoveDirection = VDirection.DOWN;
	private String itemDetailsBar = "";
	private IMediaItem itemDetailsBarItem;
	private boolean saveScrollOnClose = false;


	public DbFace(
			final FaceNavigation navigation,
			final MnContext mnContext,
			final SessionState sessionState,
			final IMediaItemList list,
			final String nodeId,
			final Player defaultPlayer) throws MorriganException {
		super(navigation);
		this.navigation = navigation;
		this.mnContext = mnContext;
		this.sessionState = sessionState;
		this.list = list;
		this.nodeId = nodeId;
		this.defaultPlayer = defaultPlayer;
		this.dbHelper = new DbHelper(navigation, mnContext, sessionState, this.defaultPlayer, this.lastActionMessage, this);
		refreshData(false);
	}

	@Override
	public void onClose () throws Exception {
		saveScrollIfRequired();
		super.onClose();
	}

	@Override
	public String getTitle() {
		return this.list.getListName();
	}

	public void restoreSavedScroll () throws MorriganException {
		try {
			final int limit = Math.max(this.list.getCount() - 1, 0);
			final int scrollTopToRestore = this.mnContext.getUserPrefs().getIntValue(PREF_SCROLL_INDEX, this.list.getSerial(), this.scrollTop);
			this.scrollTop = Math.max(Math.min(limit, scrollTopToRestore), 0);

			final int selectedItemIndexToRestore = this.mnContext.getUserPrefs().getIntValue(PREF_SELECTED_INDEX, this.list.getSerial(),
					this.scrollTop > 0 ? this.scrollTop : this.selectedItemIndex);
			setSelectedItem(selectedItemIndexToRestore);

			this.saveScrollOnClose = true;
		}
		catch (final IOException e) {
			throw new MorriganException("Failed to read saved scroll position.", e);
		}
	}

	private void saveScrollIfRequired () throws MorriganException {
		if (!this.saveScrollOnClose) return;
		try {
			this.mnContext.getUserPrefs().putValue(PREF_SCROLL_INDEX, this.list.getSerial(), this.scrollTop);
			this.mnContext.getUserPrefs().putValue(PREF_SELECTED_INDEX, this.list.getSerial(), this.selectedItemIndex);
		}
		catch (final IOException e) {
			throw new MorriganException("Failed to save scroll position.", e);
		}
	}

	public void revealItem (final IMediaItem track) throws MorriganException {
		final int i = this.mediaNode.indexOf(track);
		if (i >= 0) {
			setSelectedItem(i);
		}
		else {
			this.lastActionMessage.setLastActionMessage("Item not in view: " + track); // TODO open new DbFace here?
		}
	}

	private void refreshData (final boolean force) throws MorriganException {
		if (this.list == null) return;

		if (this.list.hasNodes()) {
			if (this.mediaNode == null || force) {
				if (this.nodeId == null) {
					this.mediaNode = this.list.getRootNode();
				}
				else {
					this.mediaNode = this.list.getNode(this.nodeId);
				}
			}
			return;
		}

		if (force) {
			this.list.forceRead();
		}
		else {
			this.list.read();
		}
		this.mediaNode = new MediaNode(null, null, null, Collections.emptyList(), this.list.getMediaItems());
	}

	private void updateItemDetailsBar () {
		final AbstractItem item = getSelectedItem();
		if (this.itemDetailsBarItem != null && this.itemDetailsBarItem.equals(item)) return;

		if (!(item instanceof IMediaItem)) return;
		final IMediaItem mi = (IMediaItem) item;

		this.mnContext.getUnreliableEs().submit(new Callable<Void>() {
			@Override
			public Void call () throws MorriganException {
				final String tags = PrintingThingsHelper.summariseItemTags(DbFace.this.list, mi);
				scheduleOnUiThread(new Callable<Void>() {
					@Override
					public Void call () {
						DbFace.this.itemDetailsBarItem = mi;
						DbFace.this.itemDetailsBar = tags;
						return null;
					}
				});
				return null;
			}
		});
	}

	@Override
	public boolean onInput (final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
		// TODO
		// - jump back all searches? (Q) (go back to last player / non search Face?).

		switch (k.getKeyType()) {
			case ArrowUp:
				menuMove(VDirection.UP, 1);
				return true;
			case ArrowDown:
				menuMove(VDirection.DOWN, 1);
				return true;
			case PageUp:
				menuMove(VDirection.UP, this.pageSize - 1);
				return true;
			case PageDown:
				menuMove(VDirection.DOWN, this.pageSize - 1);
				return true;
			case Home:
				menuMoveEnd(VDirection.UP);
				return true;
			case End:
				menuMoveEnd(VDirection.DOWN);
				return true;
			case Enter:
				itemClicked(gui);
				return true;
			case Character:
				switch (k.getCharacter()) {
					case 'h':
						this.navigation.startFace(new HelpFace(this.navigation, HELP_TEXT));
						return true;
					case 'g':
						menuMoveEnd(VDirection.UP);
						return true;
					case 'G':
						menuMoveEnd(VDirection.DOWN);
						return true;
					case 'z':
						centreSelection();
						return true;
					case 'e':
						enqueueSelection(gui);
						return true;
					case 'E':
						enqueueDb(gui);
						return true;
					case 't':
						showEditTagsForSelectedItem(gui);
						return true;
					case 'v':
					case 'x':
						toggleSelection();
						return true;
					case 'X':
						askEmptySelection(gui);
						return true;
					case 'w':
						askExportSelection(gui);
						return true;
					case 'd':
						toggleEnabledSelection();
						return true;
					case 'o':
						askSortColumn(gui);
						return true;
					case '/':
						askSearch(gui);
						return true;
					case 'p':
						if (this.list instanceof IMediaItemDb) {
							this.navigation.startFace(new DbPropertiesFace(this.navigation, this.mnContext, this.sessionState, (IMediaItemDb) this.list));
						}
						else {
							this.lastActionMessage.setLastActionMessage(String.format("Can not show properties for non-DB."));
						}
						return true;
					case 'r':
						refreshData(true);
						return true;
					default:
				}
				//$FALL-THROUGH$
			default:
				return super.onInput(k, gui);
		}
	}

	private void menuMove(final VDirection direction, final int distance) {
		setSelectedItem(MenuHelper.moveListSelectionIndex(this.selectedItemIndex, direction, distance, this.mediaNode));
		this.lastMoveDirection = direction;
	}

	private void menuMoveEnd (final VDirection direction) throws MorriganException {
		if (this.mediaNode == null || this.mediaNode.size() < 1) return;
		switch (direction) {
			case UP:
				setSelectedItem(0);
				break;
			case DOWN:
				setSelectedItem(this.mediaNode.size() - 1);
				break;
			default:
		}
	}

	private void centreSelection () {
		final int t = this.selectedItemIndex - (this.pageSize / 2);
		if (t >= 0) this.scrollTop = t;
	}

	private void setSelectedItem (final int index) {
		this.selectedItemIndex = Math.min(index, this.mediaNode.size() - 1);
		this.selectedItemIndex = Math.max(this.selectedItemIndex, 0);
		updateItemDetailsBar();
	}

	private AbstractItem getSelectedItem () {
		if (this.selectedItemIndex < 0) return null;
		if (this.selectedItemIndex >= this.mediaNode.size()) return null;
		return this.mediaNode.get(this.selectedItemIndex);
	}

	private List<IMediaItem> getSelectedItems () {
		if (this.selectedItems.size() > 0) {
			final List<IMediaItem> ret = new ArrayList<>();
			for (final AbstractItem item : this.mediaNode) {
				if (this.selectedItems.contains(item)) ret.add((IMediaItem) item);
			}
			return ret;
		}

		if (this.selectedItemIndex >= 0) {
			final AbstractItem item = this.mediaNode.get(this.selectedItemIndex);
			if (item instanceof IMediaItem) return Collections.singletonList((IMediaItem) item);
		}

		return Collections.emptyList();
	}

	private void itemClicked(final WindowBasedTextGUI gui) {
		final AbstractItem item = getSelectedItem();
		if (item instanceof MediaNode) {
			navToNode(((MediaNode) item).getId());
			return;
		}

		playSelection(gui);
	}

	private void navToNode(final String id) {
		try {
			this.navigation.startFace(new DbFace(this.navigation, this.mnContext, this.sessionState, this.list, id, this.defaultPlayer));
		}
		catch (final MorriganException e) {
			// TODO make this message more friendly.
			this.lastActionMessage.setLastActionMessage(e.toString());
		}
	}

	private Player getPlayer (final WindowBasedTextGUI gui, final String title) {
		return PlayerHelper.askWhichPlayer(gui, title, this.defaultPlayer, this.mnContext.getPlayerReader().getPlayers());
	}

	private void enqueueDb (final WindowBasedTextGUI gui) {
		final Player player = getPlayer(gui, "Enqueue DB");
		if (player != null) enqueuePlayItem(new PlayItem(this.list, null), player);
	}

	protected void enqueuePlayItem (final PlayItem playItem, final Player player) {
		player.getQueue().addToQueue(playItem);
		// TODO protect against long item names?
		this.lastActionMessage.setLastActionMessage(String.format("Enqueued %s in %s.", playItem, player.getName()));
	}

	private void enqueueSelection (final WindowBasedTextGUI gui) {
		final List<IMediaItem> items = getSelectedItems();
		enqueueItems(gui, items);
		if (items.size() == 1 && items.contains(getSelectedItem())) {
			menuMove(this.lastMoveDirection, 1);
		}
	}

	private void enqueueItems (final WindowBasedTextGUI gui, final List<IMediaItem> tracks) {
		if (tracks.size() < 1) return;
		final Player player = getPlayer(gui, String.format("Enqueue %s items", tracks.size()));
		if (player == null) return;
		PlayerHelper.enqueueAll(this.list, tracks, player);
		this.lastActionMessage.setLastActionMessage(String.format("Enqueued %s items in %s.", tracks.size(), player.getName()));
	}

	private void playSelection (final WindowBasedTextGUI gui) {
		playItems(gui, getSelectedItems());
	}

	private void playItems (final WindowBasedTextGUI gui, final List<IMediaItem> tracks) {
		if (tracks.size() < 1) return;
		final Player player = getPlayer(gui, String.format("Play %s items", tracks.size()));
		if (player == null) return;
		this.lastActionMessage.setLastActionMessage(String.format("Playing %s items in %s.", tracks.size(), player.getName()));
		PlayerHelper.playAll(this.list, tracks, player);
	}

	private void showEditTagsForSelectedItem (final WindowBasedTextGUI gui) throws MorriganException {
		final AbstractItem item = getSelectedItem();
		if (item == null) return;
		if (!(item instanceof IMediaItem)) return;
		TagEditor.show(gui, this.list, (IMediaItem) item);
	}

	private void toggleSelection () throws MorriganException {
		final AbstractItem item = getSelectedItem();
		if (item == null) return;
		if (!(item instanceof IMediaItem)) return;
		if (!this.selectedItems.remove(item)) this.selectedItems.add((IMediaItem) item);
		updateItemDetailsBar();
	}

	private void askEmptySelection (final WindowBasedTextGUI gui) throws MorriganException {
		if (MessageDialog.showMessageDialog(gui, "Selected Items", "Unselect all items?",
				MessageDialogButton.No, MessageDialogButton.Yes) != MessageDialogButton.Yes) {
			return;
		}
		this.selectedItems.clear();
	}

	private void askExportSelection (final WindowBasedTextGUI gui) {
		final List<IMediaItem> items = getSelectedItems();
		if (items.size() < 1) return;
		final File dir = DirDialog.show(gui, String.format("Export %s tracks", items.size()), "Export", this.sessionState.initialDir);
		if (dir == null) return;
		final MorriganTask task = this.mnContext.getMediaFactory().getMediaFileCopyTask(this.list, items, dir);
		this.mnContext.getAsyncTasksRegister().scheduleTask(task);
		this.lastActionMessage.setLastActionMessage(String.format("Started copying %s tracks ...", items.size()));
	}

	private void toggleEnabledSelection () throws MorriganException {
		final List<IMediaItem> items = getSelectedItems();
		for (final IMediaItem item : items) {
			this.list.setItemEnabled(item, !item.isEnabled());
		}
		this.lastActionMessage.setLastActionMessage(String.format("Toggled enabled on %s items.", items.size()));
	}

	private void askSortColumn (final WindowBasedTextGUI gui) {
		if (!(this.list instanceof IMediaItemDb)) {
			this.lastActionMessage.setLastActionMessage("Can not sort non-local DBs.");
			return;
		}
		final IMediaItemDb db = (IMediaItemDb) this.list;

		final List<IDbColumn> cols = db.getDbLayer().getMediaTblColumns();
		final List<Runnable> actions = new ArrayList<>();
		for (final IDbColumn col : cols) {
			if (col.getHumanName() != null) {
				actions.add(new SortColumnAction(db, col, SortDirection.ASC));
				actions.add(new SortColumnAction(db, col, SortDirection.DESC));
			}
		}
		final ActionListDialog dlg = new ActionListDialogBuilder()
				.setTitle("Sort Order")
				.setDescription("Current: " + PrintingThingsHelper.sortSummary(db))
				.addActions(actions.toArray(new Runnable[actions.size()]))
				.build();
		dlg.setCloseWindowWithEscape(true);
		dlg.showDialog(gui);
	}

	private void askSearch (final WindowBasedTextGUI gui) throws DbException, MorriganException {
		this.dbHelper.askSearch(gui, this.list);
	}

	@Override
	public void writeScreen (final Screen scr, final TextGraphics tg, final int top, final int bottom, final int columns) {
		if (this.list != null) {
			writeDbToScreen(scr, tg, top, bottom, columns);
		}
		else {
			tg.putString(0, 0, "Unable to show null db.");
		}
	}

	private void writeDbToScreen (final Screen scr, final TextGraphics tg, final int top, final int bottom, final int columns) {
		int l = top;

		String summary = String.format("DB %s", this.list.getListName());
		if (this.list instanceof IMediaItemDb) {
			final IMediaItemDb db = (IMediaItemDb) this.list;
			summary += ": " + PrintingThingsHelper.dbSummary(db);
			summary += "   " + PrintingThingsHelper.sortSummary(db);
		}

		tg.putString(0, l++, summary);
		this.lastActionMessage.drawLastActionMessage(tg, l++);

		this.pageSize = bottom - l;
		this.scrollTop = MenuHelper.calcScrollTop(this.pageSize, this.scrollTop, this.selectedItemIndex);

		final int colRightDuration = columns;
		final int colRightPlayCount = colRightDuration - 8;
		final int colRightLastPlayed = colRightPlayCount - 8;

		for (int i = this.scrollTop; i < this.mediaNode.size(); i++) {
			if (i < 0) break;
			if (i >= this.scrollTop + this.pageSize) break;
			if (i >= this.mediaNode.size()) break;

			final AbstractItem item = this.mediaNode.get(i);
			final String name = item.getTitle();
			final boolean invert = i == this.selectedItemIndex;

			if (item instanceof IMediaItem) {
				drawMediaItem(tg, (IMediaItem) item, name,
						invert, l,
						colRightDuration, colRightPlayCount, colRightLastPlayed);
			}
			else {
				if (invert) {
					tg.enableModifiers(SGR.REVERSE);
				}
				else {
					tg.disableModifiers(SGR.REVERSE);
				}
				tg.putString(1, l, name);
			}

			l++;
		}
		tg.disableModifiers(SGR.REVERSE);

		this.textGuiUtils.drawTextRowWithBg(tg, bottom, this.itemDetailsBar, MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);
		final String scroll = " " + PrintingThingsHelper.scrollSummary(this.mediaNode.size(), this.pageSize, this.scrollTop);
		int left = columns - scroll.length();
		this.textGuiUtils.drawTextWithBg(tg, left, bottom, scroll, MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);
		if (this.selectedItems.size() > 0) {
			final String status = String.format(" (%s)", this.selectedItems.size());
			left -= status.length();
			this.textGuiUtils.drawTextWithBg(tg, left, bottom, status, MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);

		}
	}

	private void drawMediaItem(final TextGraphics tg, final IMediaItem item, final String name, boolean invert,
			int l, final int colRightDuration, final int colRightPlayCount, final int colRightLastPlayed) {
		final boolean selectedItem = this.selectedItems.contains(item);
		if (selectedItem) {
			tg.enableModifiers(SGR.REVERSE);
		}
		else {
			tg.disableModifiers(SGR.REVERSE);
		}

		if (item.isMissing()) {
			this.textGuiUtils.drawTextWithFg(tg, 0, l, "m", TextColor.ANSI.YELLOW);
		}
		else if (!item.isEnabled()) {
			this.textGuiUtils.drawTextWithFg(tg, 0, l, "d", TextColor.ANSI.RED);
		}
		else if (selectedItem) {
			tg.putString(0, l, ">");
		}

		if (invert) {
			tg.enableModifiers(SGR.REVERSE);
		}
		else {
			tg.disableModifiers(SGR.REVERSE);
		}

		tg.putString(1, l, name);

		// Rest of item title space if selected.
		if (invert) {
			for (int x = 1 + TerminalTextUtils.getColumnWidth(name); x < colRightDuration; x++) {
				tg.setCharacter(x, l, ' ');
			}
		}

		// Warning labels.
		if (item.isMissing()) {
			this.textGuiUtils.drawTextWithFg(tg, name.length() + 2, l, "(missing)", TextColor.ANSI.YELLOW);
		}
		else if (!item.isEnabled()) {
			this.textGuiUtils.drawTextWithFg(tg, name.length() + 2, l, "(disabled)", TextColor.ANSI.RED);
		}

		// Last played column.
		if (item.getDateLastPlayed() != null) {
			final String lastPlayed = String.format(" %s", this.dateFormat.format(item.getDateLastPlayed()));
			tg.putString(colRightLastPlayed - lastPlayed.length(), l, lastPlayed);
		}

		// Play count column.
		if (item.getStartCount() > 0 || item.getEndCount() > 0) {
			final String counts = String.format("%4s/%-3s", item.getStartCount(), item.getEndCount());
			tg.putString(colRightPlayCount - counts.length(), l, counts);
		}

		// Duration column.
		if (item.getDuration() > 0) {
			final String dur = formatTimeSecondsLeftPadded(item.getDuration());
			tg.putString(colRightDuration - dur.length(), l, dur);
		}
	}

	private static String formatTimeSecondsLeftPadded (final long seconds) {
		if (seconds >= 3600) {
			return String.format(" %d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
		}
		return String.format(" %4d:%02d", (seconds % 3600) / 60, (seconds % 60));
	}

	private static class SortColumnAction implements Runnable {

		private final IMediaItemDb db;
		private final IDbColumn col;
		private final SortDirection direction;

		public SortColumnAction (final IMediaItemDb db, final IDbColumn col, final SortDirection direction) {
			this.db = db;
			this.col = col;
			this.direction = direction;
		}

		@Override
		public String toString () {
			return String.format("%s %s", this.col.getHumanName(), this.direction);
		}

		@Override
		public void run () {
			try {
				this.db.setSort(this.col, this.direction);
			}
			catch (final MorriganException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}

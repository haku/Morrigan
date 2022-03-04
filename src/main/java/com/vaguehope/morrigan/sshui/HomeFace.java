package com.vaguehope.morrigan.sshui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;
import com.vaguehope.morrigan.sshui.util.LastActionMessage;
import com.vaguehope.morrigan.sshui.util.MenuItem;
import com.vaguehope.morrigan.sshui.util.MenuItems;
import com.vaguehope.morrigan.sshui.util.SelectionAndScroll;
import com.vaguehope.morrigan.tasks.AsyncTask;

public class HomeFace extends DefaultFace {

	private static final String HELP_TEXT =
			"       g\tgo to top of list\n" +
			"       G\tgo to end of list\n" +
			" <space>\tplay / pause selected player or play DB\n" +
			"       n\tcreate new DB\n" +
			"       e\tenqueue DB\n" +
			"       /\tsearch DB\n" +
			"<ctrl>+c\tcancel task\n" +
			"       q\tback a page\n" +
			"       h\tthis help text";

	private static final long DATA_REFRESH_MILLIS = 500L;

	private static final Function<Player, String> PLAYER_TO_STRING = p -> {
		if (p.isDisposed()) return " (disposed)";
		return String.format(" %s %s %s",
				p.getName(), PrintingThingsHelper.playerStateMsg(p), PrintingThingsHelper.playingItemTitle(p));
	};

	private final FaceNavigation navigation;
	private final MnContext mnContext;
	private final DbHelper dbHelper;

	private final LastActionMessage lastActionMessage = new LastActionMessage();
	private final AtomicReference<String> savedSearchTerm = new AtomicReference<>();

	private long lastDataRefresh = 0;
	private List<Player> players;
	private List<AsyncTask> tasks;
	private List<MediaListReference> dbs;
	private MenuItems menuItems;

	private int terminalBottomRow = 1;  // zero-index terminal height.
	private SelectionAndScroll selectionAndScroll = new SelectionAndScroll(null, 0);

	public HomeFace (final FaceNavigation actions, final MnContext mnContext) {
		super(actions);
		this.navigation = actions;
		this.mnContext = mnContext;
		this.dbHelper = new DbHelper(this.navigation, mnContext, null, this.lastActionMessage, null);
	}

	private void refreshData () {
		this.players = asList(this.mnContext.getPlayerReader().getPlayers());
		this.tasks = this.mnContext.getAsyncTasksRegister().tasks();
		this.menuItems = MenuItems.builder()
				.addHeading("Players")
				.addList(this.players, " (no players)", PLAYER_TO_STRING)
				.addSubmenu(this.lastActionMessage)
				.addHeading("DBs")
				.addList(this.dbs, " (no DBs)", d -> " " + d.getTitle())
				.addHeading("")
				.addHeading("Background Tasks")
				.addList(this.tasks, " (no tasks)", t -> " " + t.oneLineSummary())
				.build();
	}

	private void refreshStaleData () {
		final long now = System.nanoTime();
		if (now - this.lastDataRefresh > TimeUnit.MILLISECONDS.toNanos(DATA_REFRESH_MILLIS)) {
			refreshData();
			this.lastDataRefresh = now;
		}
	}

	@Override
	public void onFocus() {
		this.dbs = asList(this.mnContext.getMediaFactory().getAllLocalMixedMediaDbs());
	}

	@Override
	public boolean onInput (final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
		switch (k.getKeyType()) {
			case ArrowUp:
				menuMove(-1);
				return true;
			case ArrowDown:
				menuMove(1);
				return true;
			case Home:
				menuMoveEnd(VDirection.UP);
				return true;
			case End:
				menuMoveEnd(VDirection.DOWN);
				return true;
			case Enter:
				menuEnter(gui);
				return true;
			case Character:
				switch (k.getCharacter()) {
					case 'q':
						return this.navigation.backOneLevel();
					case 'h':
						this.navigation.startFace(new HelpFace(this.navigation, HELP_TEXT));
						return true;
					case 'g':
						menuMoveEnd(VDirection.UP);
						return true;
					case 'G':
						menuMoveEnd(VDirection.DOWN);
						return true;
					case ' ':
						menuClick(gui);
						return true;
					case 'n':
						askNewDb(gui);
						return true;
					case 'e':
						enqueueDb(gui);
						return true;
					case '/':
						askSearch(gui);
						return true;
					case 'c':
						if (k.isCtrlDown()) {
							cancelSelectedTask();
							return true;
						}
						return false;
					default:
				}
			//$FALL-THROUGH$
		default:
				//LOG.info("kind={} c={} a={} char={}", k.getKind(), k.isCtrlPressed(), k.isAltPressed(), String.valueOf((int) k.getCharacter()));
				return super.onInput(k, gui);
		}
	}

	private void menuMove (final int distance) {
		if (this.menuItems == null) return;
		this.selectionAndScroll = this.menuItems.moveSelection(this.selectionAndScroll, this.terminalBottomRow + 1, distance);
	}

	private void menuMoveEnd (final VDirection direction) {
		if (this.menuItems == null) return;
		this.selectionAndScroll = this.menuItems.moveSelectionToEnd(this.selectionAndScroll, this.terminalBottomRow + 1, direction);
	}

	private void menuClick (final WindowBasedTextGUI gui) throws DbException, MorriganException {
		if (this.selectionAndScroll.selectedItem == null) return;
		if (this.selectionAndScroll.selectedItem instanceof Player) {
			((Player) this.selectionAndScroll.selectedItem).pausePlaying();
		}
		else if (this.selectionAndScroll.selectedItem instanceof MediaListReference) {
			final Player player = getPlayer(gui, "Play DB");
			if (player != null) {
				final IMixedMediaDb db = this.dbHelper.resolveReference((MediaListReference) this.selectionAndScroll.selectedItem);
				playPlayItem(new PlayItem(db, null), player);
			}
		}
		else if (this.selectionAndScroll.selectedItem instanceof AsyncTask) {
			// Do nothing.
		}
		else {
			MessageDialog.showMessageDialog(gui, "Error", "Unknown type: " + this.selectionAndScroll.selectedItem);
		}
	}

	private void menuEnter (final WindowBasedTextGUI gui) throws DbException, MorriganException {
		if (this.selectionAndScroll.selectedItem == null) return;
		if (this.selectionAndScroll.selectedItem instanceof Player) {
			this.navigation.startFace(new PlayerFace(this.navigation, this.mnContext, (Player) this.selectionAndScroll.selectedItem));
		}
		else if (this.selectionAndScroll.selectedItem instanceof MediaListReference) {
			final IMixedMediaDb db = this.dbHelper.resolveReference((MediaListReference) this.selectionAndScroll.selectedItem);
			final DbFace dbFace = new DbFace(this.navigation, this.mnContext, db, null);
			dbFace.restoreSavedScroll();
			this.navigation.startFace(dbFace);
		}
		else if (this.selectionAndScroll.selectedItem instanceof AsyncTask) {
			this.navigation.startFace(new LogFace(this.navigation, (AsyncTask) this.selectionAndScroll.selectedItem));
		}
		else {
			MessageDialog.showMessageDialog(gui, "TODO", "Enter: " + this.selectionAndScroll.selectedItem);
		}
	}

	private void askNewDb (final WindowBasedTextGUI gui) throws MorriganException {
		final String name = new TextInputDialogBuilder()
				.setTitle("New DB")
				.setDescription("Enter name:")
				.setTextBoxSize(new TerminalSize(50, 1))
				.build().showDialog(gui);
		if (name != null && name.length() > 0) {
			this.mnContext.getMediaFactory().createLocalMixedMediaDb(name);
			onFocus();
			refreshData();
		}
	}

	private void enqueueDb (final WindowBasedTextGUI gui) throws DbException, MorriganException {
		if (this.selectionAndScroll.selectedItem instanceof MediaListReference) {
			final Player player = getPlayer(gui, "Enqueue DB");
			if (player != null) {
				final IMixedMediaDb db = this.dbHelper.resolveReference((MediaListReference) this.selectionAndScroll.selectedItem);
				enqueuePlayItem(new PlayItem(db, null), player);
			}
		}
	}

	private void askSearch (final WindowBasedTextGUI gui) throws DbException, MorriganException {
		if (this.selectionAndScroll.selectedItem instanceof Player) {
			final IMediaTrackList<? extends IMediaTrack> list = ((Player) this.selectionAndScroll.selectedItem).getCurrentList();
			if (list instanceof IMixedMediaDb) {
				this.dbHelper.askSearch(gui, (IMixedMediaDb) list, this.savedSearchTerm);
			}
		}
		else if (this.selectionAndScroll.selectedItem instanceof MediaListReference) {
			final IMixedMediaDb db = this.dbHelper.resolveReference((MediaListReference) this.selectionAndScroll.selectedItem);
			this.dbHelper.askSearch(gui, db, this.savedSearchTerm);
		}
	}

	private Player getPlayer (final WindowBasedTextGUI gui, final String title) {
		return PlayerHelper.askWhichPlayer(gui, title, null, this.mnContext.getPlayerReader().getPlayers());
	}

	protected void enqueuePlayItem (final PlayItem playItem, final Player player) {
		player.getQueue().addToQueue(playItem);
		// TODO protect against long item names?
		this.lastActionMessage.setLastActionMessage(String.format("Enqueued %s in %s.", playItem, player.getName()));
	}

	protected void playPlayItem (final PlayItem playItem, final Player player) {
		player.loadAndStartPlaying(playItem);
		// TODO protect against long item names?
		this.lastActionMessage.setLastActionMessage(String.format("Playing %s in %s.", playItem, player.getName()));
	}

	@Override
	public void writeScreen (final Screen scr, final TextGraphics tg) {
		refreshStaleData();

		if (this.menuItems == null) {
			tg.putString(0, 0, "No menu items.");
			return;
		}

		final TerminalSize terminalSize = scr.getTerminalSize();
		int l = 0;

		this.terminalBottomRow = terminalSize.getRows() - 1;
		for (int i = this.selectionAndScroll.scrollTop; i < this.menuItems.size(); i++) {
			if (l > this.terminalBottomRow) break;

			final MenuItem item = this.menuItems.get(i);
			if (this.selectionAndScroll.selectedItem != null && this.selectionAndScroll.selectedItem.equals(item.getItem())) {
				tg.putString(0, l, item.toString(), SGR.REVERSE);
			}
			else {
				tg.putString(0, l, item.toString());
			}

			l++;
		}
	}

	private void cancelSelectedTask() {
		if (this.selectionAndScroll.selectedItem == null) return;
		if (this.selectionAndScroll.selectedItem instanceof AsyncTask) {
			final AsyncTask task = (AsyncTask) this.selectionAndScroll.selectedItem;
			task.cancel();
			this.lastActionMessage.setLastActionMessage(String.format("Cancelled: %s", task.title()));
		}
	}

	private static <T> List<T> asList (final Collection<T> c) {
		if (c instanceof List<?>) return (List<T>) c;
		return new ArrayList<>(c);
	}

}

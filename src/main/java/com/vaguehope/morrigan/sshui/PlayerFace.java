package com.vaguehope.morrigan.sshui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayItemType;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerQueue;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;
import com.vaguehope.morrigan.sshui.util.TextGuiUtils;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.TimeHelper;

public class PlayerFace extends DefaultFace {

	private static final String HELP_TEXT =
			"      <space>\tplay / pause\n" +
			"     <ctrl>+c\tstop\n" +
			"            C\tstop after\n" +
			"            i\tseek\n" +
			"            n\tnext track\n" +
			"            o\tplayback order\n" +
			"            e\ttranscode\n" +
			"       + OR =\tvolume up\n" +
			"            -\tvolume down\n" +
			"            /\tsearch DB\n" +
			"            T\topen tag editor for playing item\n" +
			"            g\tgo to top of list\n" +
			"            G\tgo to end of list\n" +
			"<delete> OR d\tremove from queue\n" +
			"            K\tmove to top of queue\n" +
			"            k\tmove up in queue\n" +
			"            j\tmove down in queue\n" +
			"            J\tmove to bottom of queue\n" +
			"            t\topen tag editor for queue item\n" +
			"            s\tshuffle queue\n" +
			"            q\tback a page\n" +
			"            h\tthis help text";

	private static final long DATA_REFRESH_MILLIS = 500L;
	private static final long DATA_SLOW_REFRESH_MILLIS = 10000L;

	private final FaceNavigation navigation;
	private final Player player;
	private final DbHelper dbHelper;

	private final TextGuiUtils textGuiUtils = new TextGuiUtils();
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private long lastDataRefresh = 0;
	private long lastSlowDataRefresh = 0;
	private String tagSummary;
	private PlayItem tagSummaryItem;
	private List<PlayItem> queue;
	private Object selectedItem;
	private int queueScrollTop = 0;
	private int pageSize = 1;
	private String itemDetailsBar = "";
	private Object itemDetailsBarItem;

	public PlayerFace (final FaceNavigation navigation, final MnContext mnContext, final SessionState sessionState, final Player player) {
		super(navigation);
		this.navigation = navigation;
		this.player = player;
		this.dbHelper = new DbHelper(navigation, mnContext, sessionState, player, null, null);
	}

	private void refreshData () {
		//this.player.isDisposed(); // TODO what if true?
		final PlayItem currentItem = this.player.getCurrentItem();
		if (this.tagSummaryItem == null || !this.tagSummaryItem.equals(currentItem)) {
			this.tagSummary = PrintingThingsHelper.summariseTags(this.player);
			this.tagSummaryItem = currentItem;
		}
		this.queue = this.player.getQueue().getQueueList();
	}

	private void refreshSlowData () {
		this.tagSummaryItem = null;
	}

	private void refreshStaleData () {
		final long now = System.nanoTime();
		if (now - this.lastSlowDataRefresh > TimeUnit.MILLISECONDS.toNanos(DATA_SLOW_REFRESH_MILLIS)) {
			refreshSlowData();
			this.lastSlowDataRefresh = now;
		}
		if (now - this.lastDataRefresh > TimeUnit.MILLISECONDS.toNanos(DATA_REFRESH_MILLIS)) {
			refreshData();
			this.lastDataRefresh = now;
		}
	}

	private void updateSelectedItemDetailsBar () throws MorriganException {
		if (this.itemDetailsBarItem != null && this.itemDetailsBarItem.equals(this.selectedItem)) return;
		this.itemDetailsBarItem = this.selectedItem;
		if (this.selectedItem instanceof PlayItem) {
			final PlayItem playItem = (PlayItem) this.selectedItem;
			final IMediaTrack item = playItem.getTrack();
			if (item != null) {
				this.itemDetailsBar = PrintingThingsHelper.summariseItemWithPlayCounts(playItem.getList(), item, this.dateFormat);
			}
			else {
				this.itemDetailsBar = "(no track selected)";
			}
		}
		else if (this.selectedItem != null) {
			this.itemDetailsBar = "(unknown item type)";
		}
	}

	@Override
	public String getTitle() {
		return this.player.getName();
	}

	@Override
	public boolean onInput (final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {

		// TODO
		// - add / remove tags.

		switch (k.getKeyType()) {
			case ArrowUp:
			case ArrowDown:
				menuMove(k, 1);
				return true;
			case PageUp:
			case PageDown:
				menuMove(k, this.pageSize - 1);
				return true;
			case Home:
				menuMoveEnd(VDirection.UP);
				return true;
			case End:
				menuMoveEnd(VDirection.DOWN);
				return true;
			case Delete:
				deleteQueueItem();
				return true;
			case Character:
				switch (k.getCharacter()) {
					case 'd':
						deleteQueueItem();
						return true;
					case 'q':
						return this.navigation.backOneLevel();
					case 'h':
						this.navigation.startFace(new HelpFace(this.navigation, HELP_TEXT));
						return true;
					case ' ':
						this.player.pausePlaying();
						return true;
					case 'c':
						if (k.isCtrlDown()) {
							this.player.stopPlaying();
							return true;
						}
						return false;
					case 'C':
						this.player.getQueue().addToQueueTop(this.player.getQueue().makeMetaItem(PlayItemType.STOP));
						return true;
					case 'i':
						SeekDialog.show(gui, this.player);
						return true;
					case 'n':
						this.player.nextTrack();
						return true;
					case 'o':
						askPlaybackOrder(gui);
						return true;
					case 'e':
						askTranscode(gui);
						return true;
					case '-':
						setRelativeVolume(-3);
						return true;
					case '+':
					case '=':
						setRelativeVolume(3);
						return true;
					case '/':
						askSearch(gui);
						return true;
					case 'T':
						showEditTagsForPlayingItem(gui);
						return true;
					case 't':
						showEditTagsForSelectedItem(gui);
						return true;
					case 'g':
						menuMoveEnd(VDirection.UP);
						return true;
					case 'G':
						menuMoveEnd(VDirection.DOWN);
						return true;
					case 'J':
						moveQueueItemEnd(VDirection.DOWN);
						return true;
					case 'K':
						moveQueueItemEnd(VDirection.UP);
						return true;
					case 'j':
						moveQueueItem(VDirection.DOWN);
						return true;
					case 'k':
						moveQueueItem(VDirection.UP);
						return true;
					case 's':
						askShuffleQueue(gui);
						return true;
					default:
				}
			//$FALL-THROUGH$
		default:
				return super.onInput(k, gui);
		}
	}

	private void askPlaybackOrder (final WindowBasedTextGUI gui) {
		final Runnable[] actions = new Runnable[PlaybackOrder.values().length];
		int i = 0;
		for (final PlaybackOrder po : PlaybackOrder.values()) {
			actions[i] = new Runnable() {
				@Override
				public String toString () {
					return po.toString();
				}

				@Override
				public void run () {
					PlayerFace.this.player.setPlaybackOrder(po);
				}
			};
			i++;
		}
		ActionListDialog.showDialog(gui, "Playback Order", "Current: " + this.player.getPlaybackOrder(), actions);
	}

	private void askTranscode (final WindowBasedTextGUI gui) {
		final Runnable[] actions = new Runnable[Transcode.values().length];
		int i = 0;
		for (final Transcode t : Transcode.values()) {
			actions[i] = new Runnable() {
				@Override
				public String toString () {
					return t.toString();
				}

				@Override
				public void run () {
					PlayerFace.this.player.setTranscode(t);
				}
			};
			i++;
		}
		ActionListDialog.showDialog(gui, "Transcode", "Current: " + this.player.getTranscode(), actions);
	}

	private void setRelativeVolume(final int offset) {
		final Integer curVol = this.player.getVoume();
		if (curVol == null) return;

		int newVol = curVol + offset;
		newVol = Math.min(newVol, this.player.getVoumeMaxValue());
		newVol = Math.max(newVol, 0);
		this.player.setVolume(newVol);
	}

	private void askSearch (final WindowBasedTextGUI gui) throws DbException, MorriganException {
		final IMediaTrackList<? extends IMediaTrack> list = this.player.getCurrentList();
		if (list != null) {
			if (list instanceof IMixedMediaDb) {
				askJumpTo(gui, (IMixedMediaDb) list);
			}
			else {
				MessageDialog.showMessageDialog(gui, "TODO", "Search: " + list);
			}
		}
		else {
			MessageDialog.showMessageDialog(gui, "Search", "No list selected.");
		}
	}

	private void askJumpTo (final WindowBasedTextGUI gui, final IMixedMediaDb db) throws DbException, MorriganException {
		this.dbHelper.askSearch(gui, db);
	}

	private void showEditTagsForPlayingItem (final WindowBasedTextGUI gui) throws MorriganException {
		showEditTagsForItem(gui, this.player.getCurrentItem());
	}

	private void showEditTagsForSelectedItem (final WindowBasedTextGUI gui) throws MorriganException {
		if (this.selectedItem == null) return;
		if (this.selectedItem instanceof PlayItem) {
			showEditTagsForItem(gui, (PlayItem) this.selectedItem);
		}
	}

	private static void showEditTagsForItem (final WindowBasedTextGUI gui, final PlayItem item) throws MorriganException {
		if (item == null || !item.isComplete()) return;
		TagEditor.show(gui, item.getList(), item.getTrack());
	}

	private void menuMove (final KeyStroke k, final int distance) throws MorriganException {
		this.selectedItem = MenuHelper.moveListSelection(this.selectedItem,
				k.getKeyType() == KeyType.ArrowUp || k.getKeyType() == KeyType.PageUp
						? VDirection.UP
						: VDirection.DOWN,
				distance,
				this.queue);
		updateSelectedItemDetailsBar();
	}

	private void menuMoveEnd (final VDirection direction) throws MorriganException {
		if (this.queue == null || this.queue.size() < 1) return;
		switch (direction) {
			case UP:
				this.selectedItem = this.queue.get(0);
				break;
			case DOWN:
				this.selectedItem = this.queue.get(this.queue.size() - 1);
				break;
			default:
		}
		updateSelectedItemDetailsBar();
	}

	private void moveQueueItem (final VDirection direction) {
		if (this.selectedItem == null) return;
		if (this.selectedItem instanceof PlayItem) {
			this.player.getQueue().moveInQueue(Collections.singletonList((PlayItem) this.selectedItem), direction == VDirection.DOWN);
		}
	}

	private void moveQueueItemEnd (final VDirection direction) {
		if (this.selectedItem == null) return;
		if (this.selectedItem instanceof PlayItem) {
			this.player.getQueue().moveInQueueEnd(Collections.singletonList((PlayItem) this.selectedItem), direction == VDirection.DOWN);
		}
	}

	private void askShuffleQueue(final WindowBasedTextGUI gui) {
		if (MessageDialog.showMessageDialog(gui, "Shuffle Queue?", "Shuffle Queue?",
				MessageDialogButton.No, MessageDialogButton.Yes) != MessageDialogButton.Yes) {
			return;
		}
		this.player.getQueue().shuffleQueue();
	}

	private void deleteQueueItem () {
		if (this.selectedItem == null) return;
		if (this.selectedItem instanceof PlayItem && this.queue != null) {
			final int i = this.queue.indexOf(this.selectedItem);
			this.player.getQueue().removeFromQueue((PlayItem) this.selectedItem);
			if (i >= this.queue.size()) { // Last item was deleted.
				this.queue = this.player.getQueue().getQueueList();
				if (this.queue.size() > 0) {
					this.selectedItem = this.queue.get(this.queue.size() - 1);
				}
				else {
					this.selectedItem = null;
				}
			}
			else if (i >= 0) {
				this.selectedItem = this.queue.get(i);
			}
		}
	}

	@Override
	public void writeScreen (final Screen scr, final TextGraphics tg, final int top, final int bottom, final int columns) {
		refreshStaleData();

		int l = top;

		String volMsg = PrintingThingsHelper.volumeMsg(this.player);
		if (StringHelper.notBlank(volMsg)) volMsg = volMsg + "  ";

		tg.putString(0, l++, String.format("%s:  %s%s.  %s.",
				this.player.getName(),
				volMsg,
				PrintingThingsHelper.listTitleAndOrder(this.player),
				this.player.getTranscode()));
		tg.putString(1, l++, PrintingThingsHelper.playingItemTitle(this.player));
		tg.putString(1, l++, this.tagSummary);

		final String stateMsg = PrintingThingsHelper.playerStateMsg(this.player);
		tg.putString(1, l, stateMsg);
		drawPrgBar(tg, stateMsg.length() + 2, l++, columns);

		final PlayerQueue pq = this.player.getQueue();
		tg.putString(0, l++, PrintingThingsHelper.queueSummary(pq));

		this.pageSize = bottom - l;
		this.queueScrollTop = MenuHelper.calcScrollTop(this.pageSize, this.queueScrollTop, this.queue.indexOf(this.selectedItem));

		for (int i = this.queueScrollTop; i < this.queue.size(); i++) {
			if (i >= this.queueScrollTop + this.pageSize) break;
			final PlayItem item = this.queue.get(i);

			final boolean iSelected = item.equals(this.selectedItem);
			if (iSelected) {
				tg.enableModifiers(SGR.REVERSE);
			}
			else {
				tg.disableModifiers(SGR.REVERSE);
			}

			// Item title.
			final String name = item.resolveTitle(this.player.getCurrentList());
			tg.putString(1, l, name);

			// Rest of item title space if selected.
			if (iSelected) {
				for (int x = 1 + TerminalTextUtils.getColumnWidth(name); x < columns; x++) {
					tg.setCharacter(x, l, ' ');
				}
			}

			if (item.hasTrack()) {
				final String dur = TimeHelper.formatTimeSeconds(item.getTrack().getDuration());
				tg.putString(columns - dur.length(), l, dur);
			}

			l++;
		}
		tg.disableModifiers(SGR.REVERSE);

		this.textGuiUtils.drawTextRowWithBg(tg, bottom, this.itemDetailsBar, MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);
		this.textGuiUtils.drawTextWithBg(tg, columns - 3, bottom,
				PrintingThingsHelper.scrollSummary(this.queue.size(), this.pageSize, this.queueScrollTop),
				MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);
	}

	private String lastPrgBar = null;
	private long lastPrg = -1;

	private void drawPrgBar (final TextGraphics tg, final int leftColumn, final int row, final int screenWidth) {
		final int barWidth = screenWidth - leftColumn - 1;
		final long pos = this.player.getCurrentPosition();
		final int total = this.player.getCurrentTrackDuration();

		final long prg = total < 1 || pos < 0
				? -1
				: (long) ((pos / (double) total) * barWidth);

		if (prg != this.lastPrg || this.lastPrgBar == null || this.lastPrgBar.length() != barWidth) {
			this.lastPrg = prg;
			if (prg >= 0 && barWidth > 0) {
				final StringBuilder b = new StringBuilder(barWidth);
				b.setLength(barWidth);
				for (int i = 0; i < barWidth; i++) {
					b.setCharAt(i, i < prg ? '=' : i == prg ? '>' : ' ');
				}
				this.lastPrgBar = b.toString();
			}
			else {
				this.lastPrgBar = "";
			}
		}

		tg.putString(leftColumn, row, this.lastPrgBar);
	}

}

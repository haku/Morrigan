package morrigan.sshui;

import java.util.List;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.ListRef;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;
import morrigan.player.Player;
import morrigan.sqlitewrapper.DbException;
import morrigan.sshui.Face.FaceNavigation;
import morrigan.sshui.JumpToDialog.JumpResult;
import morrigan.sshui.util.LastActionMessage;

public class DbHelper {

	private final FaceNavigation navigation;
	private final MnContext mnContext;
	private final SessionState sessionState;
	private final Player defaultPlayer;
	private final LastActionMessage lastActionMessage;
	private final DbFace defaultDbFace;

	public DbHelper (final FaceNavigation navigation, final MnContext mnContext, final SessionState sessionState, final Player defaultPlayer, final LastActionMessage lastActionMessage, final DbFace defaultDbFace) {
		this.navigation = navigation;
		this.mnContext = mnContext;
		this.sessionState = sessionState;
		this.defaultPlayer = defaultPlayer;
		this.lastActionMessage = lastActionMessage;
		this.defaultDbFace = defaultDbFace;
	}

	public MediaList resolveReference (final ListRef ref) throws DbException, MorriganException {
		final MediaList db = this.mnContext.getMediaFactory().getList(ref);
		db.read();
		return db;
	}

	public void askSearch (final WindowBasedTextGUI gui, final MediaList db) throws DbException, MorriganException {
		final JumpResult res = JumpToDialog.show(gui, db, this.sessionState);
		if (res == null) return;
		switch (res.getType()) {
			case ENQUEUE:
				enqueueItems(gui, db, res.getTracks());
				break;
			case REVEAL:
				revealItem(db, res.getTrack());
				break;
			case SHUFFLE_AND_ENQUEUE:
				shuffleAndEnqueue(gui, db, res.getTracks());
				break;
			case OPEN_VIEW:
				openFilter(db, res.getText());
				break;
			default:
		}
	}

	private void enqueueItems (final WindowBasedTextGUI gui, final MediaList db, final List<MediaItem> tracks) {
		final Player player = getPlayer(gui, String.format("Enqueue %s items", tracks.size()));
		if (player == null) return;
		PlayerHelper.enqueueAll(db, tracks, player);
		if (this.lastActionMessage != null) this.lastActionMessage.setLastActionMessage(String.format("Enqueued %s items in %s.", tracks.size(), player.getName()));
	}

	private void revealItem (final MediaList db, final MediaItem track) throws MorriganException {
		if (this.defaultDbFace != null) {
			this.defaultDbFace.revealItem(track);
		}
		else {
			final DbFace dbFace = new DbFace(this.navigation, this.mnContext, this.sessionState, db, this.defaultPlayer);
			dbFace.revealItem(track);
			this.navigation.startFace(dbFace);
		}
	}

	private void shuffleAndEnqueue (final WindowBasedTextGUI gui, final MediaList db, final List<MediaItem> tracks) {
		final Player player = getPlayer(gui, "Shuffle and enqueue");
		if (player != null) PlayerHelper.shuffleAndEnqueue(db, tracks, player);
	}

	private void openFilter (final MediaList db, final String searchTerm) throws MorriganException, DbException {
		final MediaList view = db.makeView(searchTerm);
		this.navigation.startFace(new DbFace(this.navigation, this.mnContext, this.sessionState, view, this.defaultPlayer));
	}

	private Player getPlayer (final WindowBasedTextGUI gui, final String title) {
		return PlayerHelper.askWhichPlayer(gui, title, this.defaultPlayer, this.mnContext.getPlayerReader().getPlayers());
	}

}

package com.vaguehope.morrigan.sshui;

import java.util.List;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.Face.FaceNavigation;
import com.vaguehope.morrigan.sshui.JumpToDialog.JumpResult;
import com.vaguehope.morrigan.sshui.util.LastActionMessage;

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

	public IMixedMediaDb resolveReference (final MediaListReference ref) throws DbException, MorriganException {
		final IMixedMediaDb db = this.mnContext.getMediaFactory().getMixedMediaDbByRef(ref);
		db.read();
		return db;
	}

	public void askSearch (final WindowBasedTextGUI gui, final IMixedMediaDb db) throws DbException, MorriganException {
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

	private void enqueueItems (final WindowBasedTextGUI gui, final IMediaTrackList<? extends IMediaTrack> db, final List<? extends IMediaTrack> tracks) {
		final Player player = getPlayer(gui, String.format("Enqueue %s items", tracks.size()));
		if (player == null) return;
		PlayerHelper.enqueueAll(db, tracks, player);
		if (this.lastActionMessage != null) this.lastActionMessage.setLastActionMessage(String.format("Enqueued %s items in %s.", tracks.size(), player.getName()));
	}

	private void revealItem (final IMixedMediaDb db, final IMediaTrack track) throws MorriganException {
		if (this.defaultDbFace != null) {
			this.defaultDbFace.revealItem(track);
		}
		else {
			final DbFace dbFace = new DbFace(this.navigation, this.mnContext, this.sessionState, db, this.defaultPlayer);
			dbFace.revealItem(track);
			this.navigation.startFace(dbFace);
		}
	}

	private void shuffleAndEnqueue (final WindowBasedTextGUI gui, final IMediaTrackList<? extends IMediaTrack> db, final List<? extends IMediaTrack> tracks) {
		final Player player = getPlayer(gui, "Shuffle and enqueue");
		if (player != null) PlayerHelper.shuffleAndEnqueue(db, tracks, player);
	}

	private void openFilter (final IMixedMediaDb db, final String searchTerm) throws MorriganException, DbException {
		this.navigation.startFace(new DbFace(this.navigation, this.mnContext, this.sessionState,
				this.mnContext.getMediaFactory().getLocalMixedMediaDb(db.getDbPath(), searchTerm),
				this.defaultPlayer));
	}

	private Player getPlayer (final WindowBasedTextGUI gui, final String title) {
		return PlayerHelper.askWhichPlayer(gui, title, this.defaultPlayer, this.mnContext.getPlayerReader().getPlayers());
	}

}

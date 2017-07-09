package com.vaguehope.morrigan.osgiconsole;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.AsyncActions;
import com.vaguehope.morrigan.server.model.PullRemoteToLocal;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.util.ErrorHelper;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.TimeHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class MorriganCommandProvider implements CommandProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlayerReader playerReader;
	private final MediaFactory mediaFactory;
	private final AsyncTasksRegister asyncTasksRegister;
	private final AsyncActions asyncActions;
	private final CliHelper cliHelper;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MorriganCommandProvider (final PlayerReader playerReader, final MediaFactory mediaFactory, final AsyncTasksRegister asyncTasksRegister, final AsyncActions asyncActions, final CliHelper cliHelper) {
		this.playerReader = playerReader;
		this.mediaFactory = mediaFactory;
		this.asyncTasksRegister = asyncTasksRegister;
		this.asyncActions = asyncActions;
		this.cliHelper = cliHelper;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getHelp () {
		return "---Morrigan---\n" +
				"\tmn [media|m]\n" +
				"\tmn [media|m] [create|c] <dbname>\n" +
				"\tmn [media|m] [create|c] [remote|r] <dbname> <pass>\n" +
				"\tmn [media|m] [add|a] <dir> <q1>\n" +
				"\tmn [media|m] [update|u] <q1>\n" +
				"\tmn [media|m] [sync|s] <remote q1> <local q1>\n" +
				"\tmn [media|m] [pull|p] <local q1> <remote URL>\n" +
				"\tmn [media|m] [remote|r] [add|a] <local q1> <name> <remote URL>\n" +
				"\tmn [media|m] [remote|r] [rm|r] <local q1> <name>\n" +
				"\tmn [media|m] [pull|p] <local q1> <name>\n" +
				"\tmn [media|m] albums <q1>\n" +
				"\tmn [media|m] <q1> [<q2>]\n" +
				"\tmn [players|player|p]\n" +
				"\tmn [player|p] 0 [play|queue] [<q1> [<q2>]]\n" +
				"\tmn [player|p] 0 [queue|q] clear\n" +
				"\tmn [player|p] 0 [pause|stop|next]\n" +
				"\tmn [player|p] 0 [order|o] [" + StringHelper.join(PlaybackOrder.values(), "|") + "]\n" +
				"\tmn [player|p] 0 [transcode|t] [" + StringHelper.join(Transcode.values(), "|") + "]\n" +
				"\tmn play [<q1> [<q2>]]\n" +
				"\tmn [queue|q] [<q1> [<q2>]|clear]\n" +
				"\tmn [pause|stop|s|next|n]\n" +
				"\tmn st\n" +
				"\tNOTE 1: <q1> = list, <q2> = item in <q1>.\n" +
				"\tNOTE 2: Only omit player ID when there is only one player.\n";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void _mn (final CommandInterpreter ci) {
		try {
			mnUnsafe(ci);
		}
		catch (final ArgException e) {
			ci.println(e.getMessage());
		}
		catch (final RuntimeException e) {
			ci.println(ErrorHelper.getStackTrace(e));
		}
		catch (final Exception e) {
			ci.println(ErrorHelper.getCauseTrace(e));
		}
	}

	public void mnUnsafe (final CommandInterpreter ci) throws MorriganException, IOException, ArgException, DbException, URISyntaxException {
		final List<String> args = new LinkedList<String>();
		String arg = null;
		while ((arg = ci.nextArgument()) != null) {
			args.add(arg);
		}
		if (args.size() < 1) {
			ci.println("No method specified.");
			return;
		}

		final String cmd = args.remove(0);
		if (cmd.equals("st")) {
			doStat(ci);
		}
		else if (cmd.equals("m") || cmd.equals("media")) {
			doMedia(ci, args);
		}
		else if (cmd.equals("p") || cmd.equals("players") || cmd.equals("player")) {
			doPlayers(ci, args);
		}
		else if (cmd.equals("play")) {
			doPlay(ci, args);
		}
		else if (cmd.equals("q") || cmd.equals("queue") || cmd.equals("enqueue")) {
			doQueue(ci, args);
		}
		else if (cmd.equals("pause")) {
			doPause(ci);
		}
		else if (cmd.equals("s") || cmd.equals("stop")) {
			doStop(ci);
		}
		else if (cmd.equals("n") || cmd.equals("next")) {
			doNext(ci);
		}
		else if (cmd.equals("h") || cmd.equals("help")) {
			ci.print(this.getHelp()); // already ends with new line.
		}
		else {
			ci.println("Unknown command '" + cmd + "'.");
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doStat (final CommandInterpreter ci) {
		ci.print(this.asyncTasksRegister.reportSummary()); // Has own trailing new line.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doMedia (final CommandInterpreter ci, final List<String> args) throws MorriganException, ArgException, DbException, URISyntaxException {
		if (args.size() < 1) {
			doMediaList(ci);
			return;
		}

		final String cmd = args.remove(0);
		if (args.size() > 0) {
			if (cmd.equals("u") || cmd.equals("update")) {
				doMediaScan(ci, args);
				return;
			}
			else if (cmd.equals("c") || cmd.equals("create")) {
				doMediaCreate(ci, args);
				return;
			}
			else if (cmd.equals("a") || cmd.equals("add")) {
				doMediaAdd(ci, args);
				return;
			}
			else if (cmd.equals("s") || cmd.equals("sync")) {
				doMediaSync(ci, args);
				return;
			}
			else if (cmd.equals("r") || cmd.equals("remote")) {
				doMediaRemote(ci, args);
				return;
			}
			else if (cmd.equals("p") || cmd.equals("pull")) {
				doMediaPull(ci, args);
				return;
			}
			else if (cmd.equals("albums")) {
				doMediaAlbums(ci, args);
				return;
			}
		}

		printListInfo(ci, cmd, args);
	}

	private void printListInfo (final CommandInterpreter ci, final String q1, final List<String> args) throws MorriganException, DbException, ArgException {
		final IMediaTrackList<? extends IMediaTrack> list = this.cliHelper.argQ1(q1);

		final String q2 = args.size() > 0 ? args.get(0) : null;
		if (StringHelper.notBlank(q2)) {
			final List<PlayItem> results = this.cliHelper.queryForPlayableItems(q1, q2, 10);
			if (results == null || results.size() < 1) {
				ci.println("No results for query '" + q1 + "' '" + q2 + "'.");
			}
			else {
				ci.println("Results for query:");
				for (final PlayItem pi : results) {
					ci.println(" > " + pi.toString());
				}
			}
			return;
		}

		final DurationData d = list.getTotalDuration();
		ci.print(" ");
		ci.print(String.valueOf(list.getCount()));
		ci.print(" items totaling ");
		if (!d.isComplete()) ci.print("more than ");
		ci.print(TimeHelper.formatTimeSeconds(d.getDuration()));
		ci.println(".");

		final long queryTime = list.getDurationOfLastRead();
		if (queryTime > 0) {
			ci.print(" Query took ");
			ci.print(TimeHelper.formatTimeMiliseconds(queryTime));
			ci.println(" seconds.");
		}

		if (list instanceof IMediaItemDb) {
			final IMediaItemDb<?, ?> db = (IMediaItemDb<?, ?>) list;
			for (final String s : db.getSources()) {
				ci.println(" src > " + s);
			}
			for (final Entry<String, URI> r : db.getRemotes().entrySet()) {
				ci.println(" remote > " + r.getKey() + " " + r.getValue());
			}
		}
	}

	private void doMediaList (final CommandInterpreter ci) {
		final List<MediaListReference> items = new LinkedList<MediaListReference>();
		items.addAll(this.mediaFactory.getAllLocalMixedMediaDbs());
		items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
		for (final MediaListReference i : items) {
			ci.println(i.getType() + " " + i.getTitle());
		}
	}

	private void doMediaCreate (final CommandInterpreter ci, final List<String> args) throws MorriganException, URISyntaxException {
		if (args.size() >= 1) {
			if ("remote".equals(args.get(0)) || "r".equals(args.get(0))) {
				if (args.size() >= 3) {
					final String uri = args.get(1);
					final String pass = args.get(2);
					final IRemoteMixedMediaDb db = RemoteMixedMediaDbHelper.createRemoteMmdb(uri, pass);
					ci.println("Created MMDB '" + db.getListName() + "'.");
				}
				else {
					ci.println("You must specify a URL and pass for the new remote DB.");
				}
			}
			else {
				final String name = args.get(0);
				final ILocalMixedMediaDb mmdb = this.mediaFactory.createLocalMixedMediaDb(name);
				ci.println("Created MMDB '" + mmdb.getListName() + "'.");
			}
		}
		else {
			ci.println("You must specify 'remote' or a name for the new DB.");
		}
	}

	private void doMediaAdd (final CommandInterpreter ci, final List<String> args) throws MorriganException, ArgException {
		this.cliHelper.checkArgs(args, 2);
		final File dir = this.cliHelper.argLocalDir(args.get(0));
		final ILocalMixedMediaDb ldb = this.cliHelper.argLocalQ1(args.get(1));
		ldb.addSource(dir.getAbsolutePath());
	}

	private void doMediaScan (final CommandInterpreter ci, final List<String> args) throws MorriganException, ArgException {
		this.cliHelper.checkArgs(args, 1);
		final IMediaTrackList<? extends IMediaTrack> list = this.cliHelper.argQ1(args.get(0));

		if (list instanceof ILocalMixedMediaDb) {
			this.asyncActions.scheduleMmdbScan((ILocalMixedMediaDb) list);
			ci.println("Scan scheduled.  Use 'mn st' to track progress.");
		}
		else if (list instanceof IRemoteMixedMediaDb) {
			this.asyncActions.scheduleRemoteMmdbScan((IRemoteMixedMediaDb) list);
			ci.println("Scan scheduled.  Use 'mn st' to track progress.");
		}
		else {
			ci.println("Unable to schedule scan for item '" + list.getListName() + "'.");
		}
	}

	private void doMediaSync (final CommandInterpreter ci, final List<String> args) throws MorriganException, ArgException {
		this.cliHelper.checkArgs(args, 2);
		final IRemoteMixedMediaDb rdb = this.cliHelper.argRemoteQ1(args.get(0));
		final ILocalMixedMediaDb ldb = this.cliHelper.argLocalQ1(args.get(1));

		this.asyncActions.syncMetaData(ldb, rdb);
		ci.println("Synchronisation scheduled.  Use 'mn st' to track progress.");
	}

	private void doMediaRemote (final CommandInterpreter ci, final List<String> args) throws ArgException, MorriganException, DbException {
		this.cliHelper.checkArgs(args, 3);
		final String action = args.get(0);
		final ILocalMixedMediaDb ldb = this.cliHelper.argLocalQ1(args.get(1));
		final String remoteName = this.cliHelper.argNotBlank(args.get(2));

		if ("add".equals(action) || "a".equals(action)) {
			this.cliHelper.checkArgs(args, 4);
			final URI remoteUrl = this.cliHelper.argUri(args.get(3));
			ldb.addRemote(remoteName, remoteUrl);
		}
		else if ("rm".equals(action) || "r".equals(action)) {
			ldb.rmRemote(remoteName);
		}
		else {
			ci.println("Unknown action: " + action);
		}
	}

	private void doMediaPull (final CommandInterpreter ci, final List<String> args) throws MorriganException, ArgException, DbException {
		this.cliHelper.checkArgs(args, 2);
		final ILocalMixedMediaDb ldb = this.cliHelper.argLocalQ1(args.get(0));

		final String remote = this.cliHelper.argNotBlank(args.get(1));
		URI remoteUri = ldb.getRemote(remote);
		if (remoteUri == null) {
			ci.println("Remote '" + remote + "' not found, assuming its a URI...");
			remoteUri = this.cliHelper.argUri(remote);
		}

		this.asyncTasksRegister.scheduleTask(new PullRemoteToLocal(ldb, remoteUri, this.mediaFactory));
		ci.println("Pull from " + remote + " scheduled.  Use 'mn st' to track progress.");
	}

	private void doMediaAlbums (final CommandInterpreter ci, final List<String> args) throws MorriganException, ArgException {
		this.cliHelper.checkArgs(args, 1);
		final IMediaTrackList<? extends IMediaTrack> list = this.cliHelper.argQ1(args.get(0));

		final Collection<MediaAlbum> albums = list.getAlbums();
		ci.println("Albums: (" + albums.size() + ")");
		for (final MediaAlbum album : albums) {
			ci.println(" " + album.getName() + " (" + list.getAlbumItems(album).size() + ")");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doPlayers (final CommandInterpreter ci, final List<String> args) throws MorriganException {
		if (args.size() < 1) {
			doPlayersList(ci);
			return;
		}

		final String playerId = args.remove(0);
		try {
			final Player player = this.playerReader.getPlayer(playerId);
			doPlayersPlayer(ci, player, args);
		}
		catch (final NumberFormatException e) {
			// If we only have one player, assume the next param is a cmd.
			final Player player = getOnlyPlayer(ci);
			if (player != null) {
				args.add(0, playerId);
				doPlayersPlayer(ci, player, args);
			}
			else {
				ci.println("Unknown player ID '" + playerId + "'.");
			}
		}
	}

	private void doPlayersList (final CommandInterpreter ci) {
		final Collection<Player> players = this.playerReader.getPlayers();
		ci.println("id\tplayer");
		for (final Player p : players) {
			ci.print(String.valueOf(p.getId()));
			ci.print("\t");
			ci.print(p.getName());
			ci.print(" ");
			ci.print(p.getPlayState());

			final PlayItem currentItem = p.getCurrentItem();
			if (currentItem != null && currentItem.hasTrack()) {
				ci.print(" ");
				ci.print(currentItem.getTrack().getTitle());
			}

			ci.println();
		}
	}

	private void doPlayersPlayer (final CommandInterpreter ci, final Player player, final List<String> args) throws MorriganException {
		if (args.size() < 1) {
			doPlayersPlayerInfo(ci, player);
			return;
		}

		final String cmd = args.remove(0);
		if (cmd.equals("p") || cmd.equals("play")) {
			doPlayersPlayerPlay(ci, player, args, false);
		}
		else if (cmd.equals("q") || cmd.equals("queue") || cmd.equals("enqueue")) {
			doPlayersPlayerPlay(ci, player, args, true);
		}
		else if (cmd.equals("pause")) {
			doPlayersPlayerPause(ci, player);
		}
		else if (cmd.equals("s") || cmd.equals("stop")) {
			doPlayersPlayerStop(ci, player);
		}
		else if (cmd.equals("n") || cmd.equals("next")) {
			doPlayersPlayerNext(ci, player);
		}
		else if (cmd.equals("o") || cmd.equals("order")) {
			doPlayersPlayerOrder(ci, player, args);
		}
		else if (cmd.equals("t") || cmd.equals("transcode")) {
			doPlayersPlayerTranscode(ci, player, args);
		}
		else {
			ci.println("Unknown command '" + cmd + "'.");
		}
	}

	private static void doPlayersPlayerInfo (final CommandInterpreter ci, final Player player) {
		ci.print(player.getName());
		ci.print(": ");
		ci.print(player.getPlayState().toString());
		ci.print(" (");
		ci.print(player.getPlaybackOrder().toString());
		ci.print(")");
		ci.print(" [");
		ci.print(player.getTranscode().toString());
		ci.print("]");
		ci.println();

		final PlayItem currentItem = player.getCurrentItem();
		final IMediaTrack item = (currentItem != null && currentItem.hasTrack()) ? currentItem.getTrack() : null;
		ci.print("\t item: ");
		if (item != null) {
			ci.print(item.getTitle());
			ci.print(" (");
			ci.print(TimeHelper.formatTimeSeconds(item.getDuration()));
			ci.println(")");
		}
		else {
			ci.println("[none]");
		}

		final IMediaTrackList<? extends IMediaTrack> currentList = player.getCurrentList();
		ci.print("\t list: ");
		if (currentList != null) {
			ci.print(currentList.getListName());
			ci.print(" (");
			ci.print(String.valueOf(currentList.getCount()));
			ci.println(" items)");
		}
		else {
			ci.println("[none]");
		}

		ci.print("\tqueue: ");
		ci.print(String.valueOf(player.getQueue().getQueueList().size()));
		ci.print(" items (");
		final DurationData d = player.getQueue().getQueueTotalDuration();
		if (!d.isComplete()) ci.print(("more than "));
		ci.print(TimeHelper.formatTimeSeconds(d.getDuration()));
		ci.println(")");
	}

	private void doPlayersPlayerPlay (final CommandInterpreter ci, final Player player, final List<String> args, final boolean addToQueue) throws MorriganException {
		if (args.size() < 1) {
			if (addToQueue) {
				doPlayersPlayerPrintQueue(ci, player);
			}
			else {
				if (player.getPlayState() == PlayState.PAUSED) {
					doPlayersPlayerPause(ci, player);
				}
				else if (player.getPlayState() == PlayState.PLAYING) {
					ci.println("Already playing.");
				}
				else {
					final PlayItem currentItem = player.getCurrentItem();
					if (currentItem != null) {
						player.loadAndStartPlaying(currentItem);
					}
					else {
						ci.println("Nothing to play.");
					}
				}
			}
		}
		else if (addToQueue && args.size() == 1 && args.get(0).equals("clear")) {
			player.getQueue().clearQueue();
			ci.println("Queue for " + player.getName() + " player cleared.");
		}
		else {
			final String q1 = args.get(0);
			final String q2 = args.size() >= 2 ? args.get(1) : null;

			final List<PlayItem> results = this.cliHelper.queryForPlayableItems(q1, q2, 10);

			if (results == null || results.size() < 1) {
				ci.println("No results for query '" + q1 + "' '" + q2 + "'.");
			}
			else if (results.size() == 1) {
				if (addToQueue) {
					player.getQueue().addToQueue(results.get(0));
					ci.println("Enqueued '" + results.get(0).toString() + "'.");
				}
				else {
					player.loadAndStartPlaying(results.get(0));
				}
			}
			else {
				ci.println("Multipe results for query:");
				for (final PlayItem pi : results) {
					ci.println(" > " + pi.toString());
				}
			}

		}
	}

	private static void doPlayersPlayerPause (final CommandInterpreter ci, final Player player) {
		player.pausePlaying();
		ci.println(player.getName() + " player: " + player.getPlayState().toString());
	}

	private static void doPlayersPlayerStop (final CommandInterpreter ci, final Player player) {
		player.stopPlaying();
		ci.println(player.getName() + " player: " + player.getPlayState().toString());
	}

	private static void doPlayersPlayerNext (final CommandInterpreter ci, final Player player) {
		player.nextTrack();
		final PlayItem currentItem = player.getCurrentItem();
		if (currentItem == null) {
			ci.println(player.getName() + " player: " + player.getPlayState().toString());
		}
		else {
			ci.println(player.getName() + " player: " + currentItem.getTrack().getTitle());
		}
	}

	private static void doPlayersPlayerOrder (final CommandInterpreter ci, final Player player, final List<String> args) {
		if (args.size() < 1) {
			ci.println(player.getName() + " player order = " + player.getPlaybackOrder().toString() + ".");
			ci.print("Options:");
			for (final PlaybackOrder i : PlaybackOrder.values()) {
				ci.print(" '");
				ci.print(i.toString());
				ci.print("'");
			}
			ci.println();
			return;
		}

		final PlaybackOrder po = PlaybackOrder.forceParsePlaybackOrder(args.get(0));
		if (po != null) {
			player.setPlaybackOrder(po);
			ci.println("Playback order set to '" + po.toString() + "' for " + player.getName() + " player.");
			return;
		}
		ci.println("Unknown playback order '" + args.get(0) + "'.");
	}

	private static void doPlayersPlayerTranscode (final CommandInterpreter ci, final Player player, final List<String> args) {
		if (args.size() < 1) {
			ci.println(player.getName() + " player transcode = " + player.getTranscode().toString() + ".");
			ci.print("Options:");
			for (final Transcode i : Transcode.values()) {
				ci.print(" '");
				ci.print(i.getSymbolicName());
				ci.print("'");
			}
			ci.println();
			return;
		}

		final Transcode tr = Transcode.parseOrNull(args.get(0));
		if (tr != null) {
			player.setTranscode(tr);
			ci.println("Transcode set to '" + tr.toString() + "' for " + player.getName() + " player.");
			return;
		}
		ci.println("Unknown transcode '" + args.get(0) + "'.");
	}

	private static void doPlayersPlayerPrintQueue (final CommandInterpreter ci, final Player player) {
		final List<PlayItem> queue = player.getQueue().getQueueList();

		if (queue.size() < 1) {
			ci.println("Queue for " + player.getName() + " player is empty.");
			return;
		}

		final DurationData duration = player.getQueue().getQueueTotalDuration();
		ci.println(player.getName() + " player has " + queue.size() + " items totaling " + (duration.isComplete() ? "" : " more than ") + TimeHelper.formatTimeSeconds(duration.getDuration()) + " in its queue.");
		for (final PlayItem pi : queue) {
			ci.println(" > " + pi.toString());
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Top-level shortcuts.

	private void doPlay (final CommandInterpreter ci, final List<String> args) throws MorriganException {
		final Player player = getOnlyPlayer(ci);
		if (player != null) doPlayersPlayerPlay(ci, player, args, false);
	}

	private void doQueue (final CommandInterpreter ci, final List<String> args) throws MorriganException {
		final Player player = getOnlyPlayer(ci);
		if (player != null) doPlayersPlayerPlay(ci, player, args, true);
	}

	private void doPause (final CommandInterpreter ci) {
		final Player player = getOnlyPlayer(ci);
		if (player != null) doPlayersPlayerPause(ci, player);
	}

	private void doStop (final CommandInterpreter ci) {
		final Player player = getOnlyPlayer(ci);
		if (player != null) doPlayersPlayerStop(ci, player);
	}

	private void doNext (final CommandInterpreter ci) {
		final Player player = getOnlyPlayer(ci);
		if (player != null) doPlayersPlayerNext(ci, player);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Player getOnlyPlayer (final CommandInterpreter ci) {
		final Collection<Player> allPlayers = this.playerReader.getPlayers();
		final Player player = allPlayers.size() == 1 ? allPlayers.iterator().next() : null;
		if (player == null) ci.println("There is not only one player, so you may need to specfy the player to use.");
		return player;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package com.vaguehope.morrigan.sshui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.util.LastActionMessage;
import com.vaguehope.morrigan.sshui.util.MenuItems;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.transcode.TranscodeTask;

public class DbPropertiesFace extends MenuFace {

	private static final String HELP_TEXT =
			"            g\tgo to top of list\n" +
			"            G\tgo to end of list\n" +
			"            r\trefresh\n" +
			"            n\tadd new source\n" +
			"            m\tadd new remote\n" +
			"<delete> OR d\tremove source\n" +
			"            u\trescan sources\n" +
			"            t\tpre-run transcodes\n" +
			"            q\tback a page\n" +
			"            h\tthis help text";

	private final FaceNavigation navigation;
	private final MnContext mnContext;
	private final SessionState sessionState;
	private final IMixedMediaDb db;

	private final LastActionMessage lastActionMessage = new LastActionMessage();

	private List<String> sources;
	private List<Entry<String, URI>> remotes;

	public DbPropertiesFace(
			final FaceNavigation navigation,
			final MnContext mnContext,
			final SessionState sessionState,
			final IMixedMediaDb db) throws MorriganException, DbException {
		super(navigation);
		this.navigation = navigation;
		this.mnContext = mnContext;
		this.sessionState = sessionState;
		this.db = db;
		refreshLists();
	}

	private void refreshLists() throws MorriganException, DbException {
		this.sources = this.db.getSources();
		this.remotes = new ArrayList<>(this.db.getRemotes().entrySet());
		refreshData();
	}

	@Override
	protected MenuItems refreshMenu() {
		return MenuItems.builder()
				.addHeading(String.format("DB %s:", this.db.getListName()))
				.addSubmenu(this.lastActionMessage)
				.addHeading("Sources")
				.addList(this.sources, " (no media directories)", s -> " " + s)
				.addHeading("")
				.addHeading("Remotes")
				.addList(this.remotes, " (no remotes)", r -> " " + r.getKey() + ": " + r.getValue())
				.build();
	}

	@Override
	public String getTitle() {
		return "Props(" + this.db.getListName() + ")";
	}

	@Override
	public boolean onInput (final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
		switch (k.getKeyType()) {
			case Delete:
				removeSource(gui);
				return true;
			case Character:
				switch (k.getCharacter()) {
					case 'd':
						removeSource(gui);
						return true;
					case 'h':
						this.navigation.startFace(new HelpFace(this.navigation, HELP_TEXT));
						return true;
					case 'r':
						refreshLists();
						return true;
					case 'n':
						askAddSource(gui);
						return true;
					case 'm':
						askAddRemote(gui);
						return true;
					case 'u':
						rescanSources();
						return true;
					case 't':
						prerunTranscodes(gui);
						return true;
					default:
				}
			//$FALL-THROUGH$
		default:
				return super.onInput(k, gui);
		}
	}

	private void askAddSource (final WindowBasedTextGUI gui) throws Exception {
		final File dir = DirDialog.show(gui, "Add Source", "Add", this.sessionState.initialDir);
		if (dir != null) {
			this.db.addSource(dir.getAbsolutePath());
			refreshLists();
		}
	}

	private void askAddRemote (final WindowBasedTextGUI gui) throws Exception {
		String name = null;
		URI uri = null;

		while (true) {
			name = new TextInputDialogBuilder()
					.setTitle("Local name for Remote")
					.setDescription("Enter name:")
					.setTextBoxSize(new TerminalSize(50, 1))
					.setInitialContent(name != null ? name : "")
					.build().showDialog(gui);
			if (name == null) break;
			if (name.length() > 0) break;
		}
		if (name == null) return;

		String uriStr = null;
		final URI existing = this.db.getRemote(name);
		if (existing != null) uriStr = existing.toString();

		while (true) {
			uriStr = new TextInputDialogBuilder()
					.setTitle("URI for Remote")
					.setDescription("Enter URI:")
					.setTextBoxSize(new TerminalSize(70, 1))
					.setInitialContent(uriStr != null ? uriStr : "")
					.build().showDialog(gui);
			if (uriStr == null) break;
			try {
				uri = new URI(uriStr);
				uri.toURL(); // For some actual validation.
				break;
			}
			catch (final IllegalArgumentException e) {}
			catch (final URISyntaxException e) {}
			catch (final MalformedURLException e) {}
		}
		if (uri == null) return;

		this.db.addRemote(name, uri);
		refreshLists();
	}

	private void removeSource (final WindowBasedTextGUI gui) throws Exception {
		if (this.selectionAndScroll.selectedItem == null) return;
		if (this.selectionAndScroll.selectedItem instanceof String && this.sources != null) {
			final int i = this.sources.indexOf(this.selectionAndScroll.selectedItem);
			final String source = (String) this.selectionAndScroll.selectedItem;
			if (MessageDialog.showMessageDialog(gui, "Remove Source", source, MessageDialogButton.Yes, MessageDialogButton.No) != MessageDialogButton.Yes) return;
			this.db.removeSource(source);
			refreshLists();
			fixSelectionAfterDeletion(this.sources, i);
		}
		else if (this.selectionAndScroll.selectedItem instanceof Entry && this.remotes != null) {
			final int i = this.remotes.indexOf(this.selectionAndScroll.selectedItem);
			final Entry<?, ?> entry = (Entry<?, ?>) this.selectionAndScroll.selectedItem;
			if (MessageDialog.showMessageDialog(gui, "Remove Remote", String.format("%s (%s)", entry.getKey(), entry.getValue()),
					MessageDialogButton.Yes, MessageDialogButton.No) != MessageDialogButton.Yes)
				return;
			this.db.rmRemote(entry.getKey().toString());
			refreshLists();
			fixSelectionAfterDeletion(this.remotes, i);
		}
	}

	private void fixSelectionAfterDeletion(final List<?> list, final int removedIndex) {
		if (list.size() < 1) {
			this.selectionAndScroll = this.selectionAndScroll.withSelectedItem(null);
		}
		else if (removedIndex >= list.size()) { // Last item was deleted.
			this.selectionAndScroll = this.selectionAndScroll.withSelectedItem(list.get(list.size() - 1));
		}
		else if (removedIndex >= 0) {
			this.selectionAndScroll = this.selectionAndScroll.withSelectedItem(list.get(removedIndex));
		}
	}

	// TODO replace with AsyncActions() ?
	private void rescanSources () {
		if (this.db instanceof ILocalMixedMediaDb) {
			final MorriganTask task = this.mnContext.getMediaFactory().getLocalMixedMediaDbUpdateTask((ILocalMixedMediaDb) this.db);
			if (task != null) {
				this.mnContext.getAsyncTasksRegister().scheduleTask(task);
				this.lastActionMessage.setLastActionMessage("Update started.");
			}
			else {
				this.lastActionMessage.setLastActionMessage("Unable to start update, one may already be in progress.");
			}
		}
		else {
			this.lastActionMessage.setLastActionMessage("Do not know how to refresh: " + this.db);
		}
	}

	private void prerunTranscodes(final WindowBasedTextGUI gui) throws DbException {
		final Transcode tr = new ListSelectDialogBuilder<Transcode>()
				.setTitle("Select Transcode Profile")
				.addListItems(Transcode.values())
				.build().showDialog(gui);
		if (tr == null) return;

		String filter = null;
		while (true) {
			filter = new TextInputDialogBuilder()
					.setTitle("Filter items to transcode (* for no filter)")
					.setDescription("Filter:")
					.setTextBoxSize(new TerminalSize(70, 1))
					.setInitialContent(filter != null ? filter : "")
					.build().showDialog(gui);
			if (filter == null) break;
			if (filter.length() > 0) break;
		}
		if (filter == null) return;

		IMixedMediaDb d = this.db;
		if (!"*".equals(filter)) {
			d = this.mnContext.getMediaFactory().getLocalMixedMediaDb(this.db.getDbPath(), filter);
		}

		Integer number = null;
		String numberStr = null;
		while (true) {
			numberStr = new TextInputDialogBuilder()
					.setTitle("Maximum number of files to transcode")
					.setDescription("Number:")
					.setTextBoxSize(new TerminalSize(40, 1))
					.setInitialContent(numberStr != null ? numberStr : "")
					.build().showDialog(gui);
			if (numberStr == null) break;
			try {
				number = Integer.valueOf(numberStr, 10);
				if (number > 0) break;
			}
			catch (final NumberFormatException e) {}
		}
		if (number == null) return;

		this.mnContext.getAsyncTasksRegister().scheduleTask(
				new TranscodeTask(this.mnContext.getTranscoder(), tr, d, number, this.mnContext.getConfig()));
		this.lastActionMessage.setLastActionMessage("Transcode task started.");
	}

}

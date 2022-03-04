package com.vaguehope.morrigan.sshui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;
import com.vaguehope.morrigan.sshui.util.LastActionMessage;
import com.vaguehope.morrigan.sshui.util.MenuItem;
import com.vaguehope.morrigan.sshui.util.MenuItems;
import com.vaguehope.morrigan.sshui.util.SelectionAndScroll;
import com.vaguehope.morrigan.tasks.MorriganTask;

public class DbPropertiesFace extends DefaultFace {

	private static final String HELP_TEXT =
			"       g\tgo to top of list\n" +
			"       G\tgo to end of list\n" +
			"       r\trefresh\n" +
			"       n\tadd new source\n" +
			"       m\tadd new remote\n" +
			"<delete>\tremove source\n" +
			"       u\trescan sources\n" +
			"       q\tback a page\n" +
			"       h\tthis help text";

	private final FaceNavigation navigation;
	private final MnContext mnContext;
	private final IMixedMediaDb db;

	private final LastActionMessage lastActionMessage = new LastActionMessage();
	private final AtomicReference<File> savedInitialDir;

	private List<String> sources;
	private List<Entry<String, URI>> remotes;
	private MenuItems menuItems;

	private int terminalBottomRow = 1;  // zero-index terminal height.
	private SelectionAndScroll selectionAndScroll = new SelectionAndScroll(null, 0);

	public DbPropertiesFace (final FaceNavigation navigation, final MnContext mnContext, final IMixedMediaDb db, final AtomicReference<File> savedInitialDir) throws MorriganException, DbException {
		super(navigation);
		this.navigation = navigation;
		this.mnContext = mnContext;
		this.db = db;
		this.savedInitialDir = savedInitialDir;
		refreshData();
	}

	private void refreshData () throws MorriganException, DbException {
		this.sources = this.db.getSources();
		this.remotes = new ArrayList<>(this.db.getRemotes().entrySet());

		this.menuItems = MenuItems.builder()
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
	public boolean onInput (final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
		switch (k.getKeyType()) {
			case ArrowUp:
				menuMove(-1);
				return true;
			case ArrowDown:
				menuMove(1);
				return true;
			case PageUp:
				menuMove(0 - (this.terminalBottomRow - 1));
				return true;
			case PageDown:
				menuMove(this.terminalBottomRow - 1);
				return true;
			case Home:
				menuMoveEnd(VDirection.UP);
				return true;
			case End:
				menuMoveEnd(VDirection.DOWN);
				return true;
			case Delete:
				removeSource(gui);
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
					case 'r':
						refreshData();
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
					default:
				}
			//$FALL-THROUGH$
		default:
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

	private void askAddSource (final WindowBasedTextGUI gui) throws MorriganException, DbException {
		final File dir = DirDialog.show(gui, "Add Source", "Add", this.savedInitialDir);
		if (dir != null) {
			this.db.addSource(dir.getAbsolutePath());
			refreshData();
		}
	}

	private void askAddRemote (final WindowBasedTextGUI gui) throws MorriganException, DbException {
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
		refreshData();
	}

	private void removeSource (final WindowBasedTextGUI gui) throws MorriganException, DbException {
		if (this.selectionAndScroll.selectedItem == null) return;
		if (this.selectionAndScroll.selectedItem instanceof String && this.sources != null) {
			final int i = this.sources.indexOf(this.selectionAndScroll.selectedItem);
			final String source = (String) this.selectionAndScroll.selectedItem;
			if (MessageDialog.showMessageDialog(gui, "Remove Source", source, MessageDialogButton.Yes, MessageDialogButton.No) != MessageDialogButton.Yes) return;
			this.db.removeSource(source);
			refreshData();
			fixSelectionAfterDeletion(this.sources, i);
		}
		else if (this.selectionAndScroll.selectedItem instanceof Entry && this.remotes != null) {
			final int i = this.remotes.indexOf(this.selectionAndScroll.selectedItem);
			final Entry<?, ?> entry = (Entry<?, ?>) this.selectionAndScroll.selectedItem;
			if (MessageDialog.showMessageDialog(gui, "Remove Remote", String.format("%s (%s)", entry.getKey(), entry.getValue()),
					MessageDialogButton.Yes, MessageDialogButton.No) != MessageDialogButton.Yes)
				return;
			this.db.rmRemote(entry.getKey().toString());
			refreshData();
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

	@Override
	public void writeScreen (final Screen scr, final TextGraphics tg) {
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

}

package com.vaguehope.morrigan.sshui;

import java.util.Arrays;
import java.util.List;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

public class AutocompletePopup extends BasicWindow {

	private static final int MAX_LIST_ROWS = 8;

	@SuppressWarnings("resource")
	public static AutocompletePopup makeAndShow(final TextBox textBox, final List<Runnable> actions, final Runnable onClose) {
		final AutocompletePopup ap = new AutocompletePopup(textBox, actions, onClose);

		final TerminalPosition pos = textBox.toGlobal(new TerminalPosition(textBox.getCursorLocation().getColumn() - 1, 1));
		final WindowBasedTextGUI gui = (WindowBasedTextGUI) textBox.getTextGUI();
		final int x = Math.min(
				pos.getColumn(),
				gui.getScreen().getTerminalSize().getColumns() - ap.getListWidth() - 3);
		ap.setPosition(pos.withColumn(x));

		gui.addWindow(ap);
		return ap;
	}

	private final TextBox textBox;
	private final ActionListBox list;
	private final Runnable onClose;
	private final int listWidth;

	public AutocompletePopup(final TextBox textBox, final List<Runnable> actions, final Runnable onClose) {
		super();
		this.textBox = textBox;
		this.onClose = onClose;

		setHints(Arrays.asList(
				Hint.NO_FOCUS,
				Hint.FIXED_POSITION,
				Hint.MENU_POPUP));

		final int maxLength = textBox.getSize().getColumns();
		int longest = 10;
		for (final Runnable a : actions) {
			final int aLen = a.toString().length();
			if (aLen > maxLength) {
				longest = maxLength;
				break;
			}
			longest = Math.max(longest, aLen);
		}
		this.listWidth = longest;

		this.list = new ActionListBox(textBox.getSize()
				.withRows(Math.min(actions.size(), MAX_LIST_ROWS))
				.withColumns(this.listWidth));

		for (final Runnable a : actions) {
			this.list.addItem(new Runnable() {
				@Override
				public void run() {
					a.run();
					close();
				}

				@Override
				public String toString() {
					return a.toString();
				}
			});
		}
		this.list.setSelectedIndex(0);
		setComponent(this.list);
	}

	@Override
	public void close() {
		super.close();
		this.onClose.run();
	}

	public int getListWidth() {
		return this.listWidth;
	}

	public boolean offerInput(final KeyStroke key) {
		switch (key.getKeyType()) {
		case Escape:
			close();
			return true;
		case ArrowUp:
		case ArrowDown:
		case Enter:
			this.list.handleKeyStroke(key);
			return true;
		case Tab:
			this.list.handleInput(new KeyStroke(KeyType.ArrowDown));
			return true;
		case ReverseTab:
			this.list.handleInput(new KeyStroke(KeyType.ArrowUp));
			return true;
		default:
			return false;
		}
	}

	@Override
	public synchronized boolean handleInput(final KeyStroke keyStroke) {
		if (offerInput(keyStroke)) {
			return true;
		}
		return super.handleInput(keyStroke);
	}

	@Override
	public synchronized Theme getTheme() {
		return this.textBox.getTheme();
	}

}
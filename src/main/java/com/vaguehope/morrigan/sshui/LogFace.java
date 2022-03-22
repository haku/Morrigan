package com.vaguehope.morrigan.sshui;

import java.util.List;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;
import com.vaguehope.morrigan.sshui.util.TextGuiUtils;
import com.vaguehope.morrigan.tasks.AsyncTask;

public class LogFace extends DefaultFace {

	private final FaceNavigation navigation;
	private final AsyncTask task;
	private final List<String> messages;

	private final TextGuiUtils textGuiUtils = new TextGuiUtils();

	private Object selectedItem;
	private int scrollTop = 0;
	private int pageSize = 1;

	public LogFace(final FaceNavigation navigation, final AsyncTask task) {
		super(navigation);
		this.navigation = navigation;
		this.task = task;
		this.messages = task.getAllMessages();
	}

	@Override
	public String getTitle() {
		return this.task.title();
	}

	@Override
	public boolean onInput(final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
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
			case Character:
				switch (k.getCharacter()) {
					case 'q':
						return this.navigation.backOneLevel();
					case 'g':
						menuMoveEnd(VDirection.UP);
						return true;
					case 'G':
						menuMoveEnd(VDirection.DOWN);
						return true;
					default:
				}
			//$FALL-THROUGH$
		default:
			return super.onInput(k, gui);
		}
	}

	private void menuMove (final KeyStroke k, final int distance) {
		this.selectedItem = MenuHelper.moveListSelection(this.selectedItem,
				k.getKeyType() == KeyType.ArrowUp || k.getKeyType() == KeyType.PageUp
						? VDirection.UP
						: VDirection.DOWN,
				distance,
				this.messages);
	}

	private void menuMoveEnd (final VDirection direction) {
		if (this.messages == null || this.messages.size() < 1) return;
		switch (direction) {
			case UP:
				this.selectedItem = this.messages.get(0);
				break;
			case DOWN:
				this.selectedItem = this.messages.get(this.messages.size() - 1);
				break;
			default:
		}
	}

	@Override
	public void writeScreen(final Screen scr, final TextGraphics tg, final int top, final int bottom, final int columns) {
		int l = top;

		this.pageSize = bottom - l;
		final int selI = this.messages.indexOf(this.selectedItem);
		if (selI >= 0) {
			if (selI - this.scrollTop >= this.pageSize) {
				this.scrollTop = selI - this.pageSize + 1;
			}
			else if (selI < this.scrollTop) {
				this.scrollTop = selI;
			}
		}

		for (int i = this.scrollTop; i < this.messages.size(); i++) {
			if (i > this.scrollTop + this.pageSize) break;
			final String item = this.messages.get(i);
			if (item.equals(this.selectedItem)) {
				tg.putString(1, l++, String.valueOf(item), SGR.REVERSE);
			}
			else {
				tg.putString(1, l++, String.valueOf(item));
			}
		}

		final String itemDetailsBar = this.messages.size() + " rows.";
		this.textGuiUtils.drawTextRowWithBg(tg, bottom, itemDetailsBar, MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);
		this.textGuiUtils.drawTextWithBg(tg, columns - 3, bottom,
				PrintingThingsHelper.scrollSummary(this.messages.size(), this.pageSize, this.scrollTop),
				MnTheme.STATUSBAR_FOREGROUND, MnTheme.STATUSBAR_BACKGROUND);
	}

}

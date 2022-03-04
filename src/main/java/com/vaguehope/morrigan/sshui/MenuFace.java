package com.vaguehope.morrigan.sshui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;
import com.vaguehope.morrigan.sshui.util.MenuItem;
import com.vaguehope.morrigan.sshui.util.MenuItems;
import com.vaguehope.morrigan.sshui.util.SelectionAndScroll;

public abstract class MenuFace extends DefaultFace {

	private MenuItems menuItems;
	private int terminalBottomRow = 1; // zero-index terminal height.
	protected SelectionAndScroll selectionAndScroll = new SelectionAndScroll(null, 0);

	public MenuFace(final FaceNavigation navigation) {
		super(navigation);
	}

	protected abstract MenuItems refreshMenu();

	protected void refreshData() {
		this.menuItems = refreshMenu();
	}

	@Override
	public boolean onInput(final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
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
		case Character:
			switch (k.getCharacter()) {
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

	private void menuMove(final int distance) {
		if (this.menuItems == null) return;
		this.selectionAndScroll = this.menuItems.moveSelection(this.selectionAndScroll, this.terminalBottomRow + 1, distance);
	}

	private void menuMoveEnd(final VDirection direction) {
		if (this.menuItems == null) return;
		this.selectionAndScroll = this.menuItems.moveSelectionToEnd(this.selectionAndScroll, this.terminalBottomRow + 1, direction);
	}

	@Override
	public void writeScreen(final Screen scr, final TextGraphics tg) {
		if (this.menuItems == null) {
			tg.putString(0, 0, "No menu items.");
			return;
		}

		int l = 0;
		this.terminalBottomRow = scr.getTerminalSize().getRows() - 1;
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

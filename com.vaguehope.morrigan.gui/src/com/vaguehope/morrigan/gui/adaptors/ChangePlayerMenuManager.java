package com.vaguehope.morrigan.gui.adaptors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.views.AbstractPlayerView;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;

public class ChangePlayerMenuManager {

	public static MenuManager create (final AbstractPlayerView pv) {
		final MenuManager mm = new MenuManager("Change player", "changePlayer");
		mm.setRemoveAllWhenShown(true);
		mm.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow (final IMenuManager manager) {
				manager.add(new ChangePlayerAction(null, pv));
				for (final Player p : Activator.getPlayerRegister().getAll()) {
					manager.add(new ChangePlayerAction(p, pv));
				}
			}
		});
		return mm;
	}

	private static class ChangePlayerAction extends Action {

		private final Player player;
		private final AbstractPlayerView pv;

		public ChangePlayerAction (final Player player, final AbstractPlayerView pv) {
			super(makeTitle(player));
			this.player = player;
			this.pv = pv;
			if (pv.isCurrentPlayer(player)) setChecked(true);
		}

		private static String makeTitle (final Player p) {
			if (p == null) return "(new player)";
			String label = p.getName();
			final PlayItem item = p.getCurrentItem();
			if (item != null && item.hasTrack()) label += ": " + item.getTrack().getTitle();
			return label;
		}

		@Override
		public void run () {
			try {
				if (this.player == null || !this.player.isDisposed()) {
					this.pv.changePlayer(this.player);
				}
				else {
					new MorriganMsgDlg("Player " + this.player + " is no longer available.").open();
				}
			}
			catch (final Exception e) { // NOSONAR only way to report failure.
				new MorriganMsgDlg(e).open();
			}
		}

	}

}

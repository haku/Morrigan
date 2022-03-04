package com.vaguehope.morrigan.sshui.util;

import com.googlecode.lanterna.graphics.TextGraphics;

public class LastActionMessage implements Submenu {

	private static final long LAST_ACTION_MESSAGE_DURATION_MILLIS = 5000L;

	private String lastActionMessage = null;
	private long lastActionMessageTime = 0;

	public void setLastActionMessage (final String lastActionMessage) {
		this.lastActionMessage = lastActionMessage;
		this.lastActionMessageTime = System.currentTimeMillis();
	}

	public void drawLastActionMessage (final TextGraphics tg, final int line) {
		final String s = stringForItem(0);
		if (s.length() > 0) {
			tg.putString(0, line, s);
		}
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Object get(final int index) {
		return null;
	}

	@Override
	public int indexOf(Object item) {
		return -1;
	}

	@Override
	public String stringForItem(final int index) {
		if (this.lastActionMessage != null && System.currentTimeMillis() - this.lastActionMessageTime > LAST_ACTION_MESSAGE_DURATION_MILLIS) {
			this.lastActionMessage = null;
		}
		if (this.lastActionMessage != null && this.lastActionMessage.length() > 0) {
			return String.format(">> %s", this.lastActionMessage);
		}
		return "";
	}

}

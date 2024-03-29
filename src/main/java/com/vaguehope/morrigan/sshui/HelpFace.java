package com.vaguehope.morrigan.sshui;

import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;

public class HelpFace extends DefaultFace {

	private final String helpText;

	public HelpFace (final FaceNavigation navigation, final String helpText) {
		super(navigation);
		this.helpText = helpText;
	}

	@Override
	public String getTitle() {
		return "Help";
	}

	@Override
	public void writeScreen (final Screen scr, final TextGraphics tg, int top, int bottom, int columns) {
		int l = top;
		for (final String line : this.helpText.split("\n")) {
			tg.putString(0, l++, line);
		}
	}

}

package com.vaguehope.morrigan.sshui;

import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;

public interface Face {

	interface FaceNavigation {

		void startFace (Face face);

		/**
		 * Returns true if was not the last face.
		 * If true is returned screen needs redrawing.
		 */
		boolean backOneLevel ();

	}

	String getTitle();

	/**
	 * Called when the face is opened or returned to.
	 */
	void onFocus() throws Exception;

	boolean onInput (KeyStroke k, WindowBasedTextGUI gui) throws Exception;

	boolean processEvents ();

	/**
	 *
	 * @param top first row to write to
	 * @param bottom bottom row of screen, inclusive,
	 */
	void writeScreen (Screen scr, TextGraphics tg, int top, int bottom, int columns);

	/**
	 * Called once when face is closed.
	 */
	void onClose () throws Exception;

}

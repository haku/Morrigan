package com.vaguehope.morrigan.sshui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.AbstractComponent;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.Terminal;
import com.vaguehope.morrigan.sshui.Face.FaceNavigation;
import com.vaguehope.morrigan.sshui.term.SshScreen;
import com.vaguehope.morrigan.util.ErrorHelper;

public class MnScreen extends SshScreen implements FaceNavigation {

	private static final Logger LOG = LoggerFactory.getLogger(MnScreen.class);

	private final MnContext mnContext;

	private final List<Deque<Face>> tabsAndFaces = new ArrayList<>();
	private int activeTab = 0;
	private WindowBasedTextGUI gui;

	public MnScreen (final String name, final MnContext mnContext, final Environment env, final Terminal terminal, final ExitCallback callback) throws IOException {
		super(name, env, terminal, callback);
		this.mnContext = mnContext;
	}

	@Override
	protected void initScreen (final Screen scr) {
		scr.setCursorPosition(null);
		this.gui = new MultiWindowTextGUI(scr, new DefaultWindowManager(), new SelfBackground(scr));
		this.gui.setTheme(MnTheme.makeTheme());
		newTab();
	}

	private class SelfBackground extends AbstractComponent<SelfBackground> {

		private final Screen scr;
		private final ComponentRenderer<SelfBackground> renderer = new ComponentRenderer<SelfBackground>() {
			@Override
			public TerminalSize getPreferredSize (final SelfBackground component) {
				return TerminalSize.ONE;
			}

			@Override
			public void drawComponent (final TextGUIGraphics graphics, final SelfBackground component) {
				// This is needed so that UI continues to update behind windows.
				// We can jump direct to writeScreen() as if no draw was needed this method is not called.
				writeScreen(SelfBackground.this.scr, graphics);
				recordTickHappened();
			}
		};

		public SelfBackground (final Screen scr) {
			this.scr = scr;
		}

		@Override
		public boolean isInvalid() {
			return super.isInvalid() | isTickNeeded();
		}

		@Override
		protected ComponentRenderer<SelfBackground> createDefaultRenderer () {
			return this.renderer;
		}
	}

	private void newTab() {
		this.tabsAndFaces.add(new LinkedList<>());
		this.activeTab = this.tabsAndFaces.size() - 1;
		startFace(new HomeFace(this, this.mnContext));
	}

	private void closeTab() {
		this.tabsAndFaces.remove(this.activeTab);
		if (this.activeTab >= this.tabsAndFaces.size()) {
			this.activeTab = this.tabsAndFaces.size() - 1;
		}
	}

	private Deque<Face> activeTab() {
		return this.tabsAndFaces.get(this.activeTab);
	}

	private void nextTab() {
		this.activeTab += 1;
		if (this.activeTab >= this.tabsAndFaces.size()) {
			this.activeTab = 0;
		}
	}

	private void prevTab() {
		this.activeTab -= 1;
		if (this.activeTab < 0) {
			this.activeTab = this.tabsAndFaces.size() - 1;
		}
	}


	private Face activeFace () {
		return activeTab().getLast();
	}

	@Override
	public void startFace (final Face face) {
		activeTab().add(face);
		face.onFocus();
	}

	@Override
	public boolean backOneLevel () {
		if (this.tabsAndFaces.size() > 1) {
			closeTab();
			return true;
		}
		if (activeTab().size() <= 1) {
			scheduleQuit("user quit");
			return false;
		}
		final Face removedFace = activeTab().removeLast();
		if (removedFace != null) {
			try {
				removedFace.onClose();
			}
			catch (final Exception e) {
				LOG.warn("onClose() failed.", e);
			}
		}
		activeFace().onFocus();
		return true;
	}

	@Override
	protected boolean onInput (final KeyStroke k) throws IOException {
		try {
			switch (k.getKeyType()) {
			case Tab:
				if (k.isShiftDown()) {
					prevTab();
					return true;
				}
				nextTab();
				return true;
			case Character:
				switch (k.getCharacter()) {
				case 't':
					if (k.isCtrlDown()) {
						newTab();
						return true;
					}
					//$FALL-THROUGH$
				default:
					break;
				}
				//$FALL-THROUGH$
			default:
				break;
			}
			return activeFace().onInput(k, this.gui);
		}
		catch (final Exception e) {
			LOG.warn("Unhandled exception while processing user input.", e);
			MessageDialog.showMessageDialog(this.gui, e.getClass().getName(), ErrorHelper.getCauseTrace(e));
			return true;
		}
	}

	@Override
	protected boolean processEvents () {
		return activeFace().processEvents();
	}

	@Override
	protected void writeScreen (final Screen scr, final TextGraphics tg) {
		final TerminalSize ts = scr.getTerminalSize();
		int top = 0;
		if (this.tabsAndFaces.size() > 1) {
			int x = 0;
			for (int i = 0; i < this.tabsAndFaces.size(); i++) {
				final String name = "T" + i;  // TODO tab names.
				if (i == this.activeTab) {
					tg.enableModifiers(SGR.REVERSE);
				}
				else {
					tg.disableModifiers(SGR.REVERSE);
				}
				tg.putString(x, 0, name);
				x += name.length() + 1;
			}
			tg.disableModifiers(SGR.REVERSE);
			top = 1;
		}
		activeFace().writeScreen(scr, tg, top, ts.getRows() - 1, ts.getColumns());
	}

}

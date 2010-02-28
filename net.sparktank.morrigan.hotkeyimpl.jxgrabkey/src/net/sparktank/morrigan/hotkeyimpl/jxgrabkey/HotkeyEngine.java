package net.sparktank.morrigan.hotkeyimpl.jxgrabkey;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import jxgrabkey.HotkeyListener;
import jxgrabkey.JXGrabKey;
import net.sparktank.morrigan.engines.hotkey.HotkeyException;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;


public class HotkeyEngine implements IHotkeyEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private File[] classPath;
	
	private IHotkeyListener listener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public HotkeyEngine () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IHotkeyEngine methods.
	
	@Override
	public String getAbout () {
		return "net.sparktank.morrigan.hotkeyimpl.jintellitype version 0.01.";
	}
	
	@Override
	public void setClassPath(File[] classPath) {
		this.classPath = classPath;
	}
	
	@Override
	public void registerHotkey(int id, HotkeyValue value) throws HotkeyException {
		try {
			loadSo();
		} catch (IOException e) {
			throw new HotkeyException("Error loading .so.", e);
		}
		
		setup();
		
		int mask = 0;
		if (value.getCtrl()) mask += KeyEvent.CTRL_MASK;
		if (value.getShift()) mask += KeyEvent.SHIFT_MASK;
		if (value.getAlt()) mask += KeyEvent.ALT_MASK;
		if (value.getSupr()) mask += KeyEvent.META_MASK;
		
		try {
			JXGrabKey.getInstance().registerAwtHotkey(id, mask, value.getKey());
		} catch (Throwable t) {
			throw new HotkeyException("Failed to register hotkey " + mask + "+" + value.getKey(), t);
		}
	}
	
	@Override
	public void unregisterHotkey(int id) throws HotkeyException {
		if (!soLoaded || !haveSetup) return;
		JXGrabKey.getInstance().unregisterHotKey(id);
	}
	
	@Override
	public void setListener(IHotkeyListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void finalise() {
		teardown();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	boolean haveSetup = false;
	
	private void setup () {
		if (haveSetup) return;
		
		JXGrabKey.setDebugOutput(true);
		JXGrabKey.getInstance().addHotkeyListener(hotkeyListener);
		
		haveSetup = true;
	}
	
	private void teardown () {
		if (!haveSetup) return;
		
		JXGrabKey.getInstance().removeHotkeyListener(hotkeyListener);
		
		haveSetup = false;
	}
	
	private HotkeyListener hotkeyListener = new jxgrabkey.HotkeyListener(){
		public void onHotkey(int hotkey_idx) {
			callListener(hotkey_idx);
		}
	};
	
	private void callListener(int id) {
		if (listener!=null) {
			listener.onKeyPress(id);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String soName = "libJXGrabKey.so";
	
	private boolean soLoaded = false;
	
	private void loadSo () throws IOException {
		if (soLoaded) return;
		
		File soFile = null;
		
		for (File classPathFile : classPath) {
			if (classPathFile.isDirectory()) {
				File[] listFiles = classPathFile.listFiles();
				if (listFiles!=null && listFiles.length>0) {
					for (File file : listFiles) {
						if (file.isFile()) {
							if (file.getName().equals(soName)) {
								soFile = file;
								break;
							}
						}
					}
				}
			}
		}
		
		if (soFile==null) {
			System.out.println("Did not find '" + soName + "'.");
			return;
		}
		System.out.println("so " + soName + "=" + soFile.getAbsolutePath());
		
		System.load(soFile.getCanonicalPath());
		
		System.out.println("loaded so=" + soFile.getAbsolutePath());
		
		soLoaded = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

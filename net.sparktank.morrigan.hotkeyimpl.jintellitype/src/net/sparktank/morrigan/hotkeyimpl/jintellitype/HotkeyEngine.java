package net.sparktank.morrigan.hotkeyimpl.jintellitype;

import java.io.File;
import java.lang.reflect.Field;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;

import net.sparktank.morrigan.engines.hotkey.HotkeyException;
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
	public void registerHotkey(int id, int key, boolean ctrl, boolean shift, boolean alt, boolean supr) throws HotkeyException {
		shoeHorn();
		
		if (!JIntellitype.isJIntellitypeSupported()) {
			throw new HotkeyException("JIntellitype is not available.");
		}
		
		setup();
		
		int mask = 0;
		if (ctrl) mask += JIntellitype.MOD_CONTROL;
		if (shift) mask += JIntellitype.MOD_SHIFT;
		if (alt) mask += JIntellitype.MOD_ALT;
		if (supr) mask += JIntellitype.MOD_WIN;
		
		try {
			JIntellitype.getInstance().registerHotKey(id, mask, key);
		} catch (Throwable t) {
			throw new HotkeyException("Failed to register hotkey " + mask + "+" +key, t);
		}
	}
	
	@Override
	public void unregisterHotkey(int id) throws HotkeyException {
		JIntellitype.getInstance().unregisterHotKey(id);
	}
	
	@Override
	public void setListener(IHotkeyListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void finalise() {
		teardown();
		if (haveShoeHorned) {
			JIntellitype.getInstance().cleanUp();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	boolean haveSetup = false;
	
	private void setup () {
		if (haveSetup) return;
		JIntellitype.getInstance().addHotKeyListener(hotkeyListener);
	}
	
	private void teardown () {
		if (!haveSetup) return;
		JIntellitype.getInstance().removeHotKeyListener(hotkeyListener);
	}
	
	private HotkeyListener hotkeyListener = new HotkeyListener() {
		@Override
		public void onHotKey(int identifier) {
			callListener(identifier);
		}
	};
	
	private void callListener(int id) {
		if (listener!=null) {
			listener.onKeyPress(id);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String dllName = "JIntellitype.dll";
	
	private boolean haveShoeHorned = false;
	
	// FIXME this is all REALLY nasty.
	@SuppressWarnings("unchecked")
	private void shoeHorn () {
		if (haveShoeHorned) return;
		
		File dllFile = null;
		
		for (File classPathFile : classPath) {
			if (classPathFile.isDirectory()) {
				File[] listFiles = classPathFile.listFiles();
				if (listFiles!=null && listFiles.length>0) {
					for (File file : listFiles) {
						if (file.isFile()) {
							if (file.getName().equals(dllName)) {
								dllFile = file;
								break;
							}
						}
					}
				}
			}
		}
		
		if (dllFile==null) {
			System.out.println("Did not find '" + dllName + "'.");
			return;
		}
		System.out.println("dll " + dllName + "=" + dllFile.getAbsolutePath());
		
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible) field.setAccessible(true);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.setProperty("java.library.path", dllFile.getParentFile().getAbsolutePath());
		
		/* FIXME
		 * This next line fails with
		 * java.lang.UnsatisfiedLinkError: Native Library D:\haku\development\eclipseWorkspace-java\dsjtest\lib\dsj.dll already loaded in another classloader
		 * if it is already loaded.
		 */
		try {
			System.load(dllFile.getAbsolutePath());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		System.out.println("loaded dll=" + dllFile.getAbsolutePath());
		
		haveShoeHorned = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

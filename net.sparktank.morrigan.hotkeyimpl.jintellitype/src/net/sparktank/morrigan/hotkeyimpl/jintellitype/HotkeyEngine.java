package net.sparktank.morrigan.hotkeyimpl.jintellitype;

import java.io.File;
import java.lang.reflect.Field;

import net.sparktank.morrigan.engines.hotkey.HotkeyException;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;

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
		shoeHorn();
		
		if (!JIntellitype.isJIntellitypeSupported()) {
			throw new HotkeyException("JIntellitype is not available.");
		}
		
		setup();
		
		int mask = 0;
		if (value.getCtrl()) mask += JIntellitype.MOD_CONTROL;
		if (value.getShift()) mask += JIntellitype.MOD_SHIFT;
		if (value.getAlt()) mask += JIntellitype.MOD_ALT;
		if (value.getSupr()) mask += JIntellitype.MOD_WIN;
		
		try {
			JIntellitype.getInstance().registerHotKey(id, mask, value.getKey());
		} catch (Throwable t) {
			throw new HotkeyException("Failed to register hotkey " + mask + "+" + value.getKey(), t);
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
		JIntellitype.getInstance().addIntellitypeListener(intellitypeListener);
		haveSetup = true;
	}
	
	private void teardown () {
		if (!haveSetup) return;
		JIntellitype.getInstance().removeHotKeyListener(hotkeyListener);
		JIntellitype.getInstance().removeIntellitypeListener(intellitypeListener);
		haveSetup = false;
	}
	
	private HotkeyListener hotkeyListener = new HotkeyListener() {
		@Override
		public void onHotKey(int identifier) {
			callListener(identifier);
		}
	};
	
	private IntellitypeListener intellitypeListener = new IntellitypeListener() {
		@Override
		public void onIntellitype(int aCommand) {
			switch (aCommand) {
				
				case JIntellitype.APPCOMMAND_MEDIA_STOP:
					callListener(MORRIGAN_HK_STOP);
					break;
					
				case JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE:
					callListener(MORRIGAN_HK_PLAYPAUSE);
					break;
					
				case JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK:
					callListener(MORRIGAN_HK_NEXT);
					break;
				
			}
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
			System.err.println("Did not find '" + dllName + "'.");
			return;
		}
		System.err.println("dll " + dllName + "=" + dllFile.getAbsolutePath());
		
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible) field.setAccessible(true);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String newLibPath = System.getProperty("java.library.path") + File.pathSeparator + dllFile.getParentFile().getAbsolutePath();
		System.err.println("Setting java.library.path=" + newLibPath);
		System.setProperty("java.library.path", newLibPath);
		
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
		
		System.err.println("loaded dll=" + dllFile.getAbsolutePath());
		
		haveShoeHorned = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

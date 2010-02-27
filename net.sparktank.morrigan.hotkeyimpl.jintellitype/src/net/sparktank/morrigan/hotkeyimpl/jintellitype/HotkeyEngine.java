package net.sparktank.morrigan.hotkeyimpl.jintellitype;

import java.io.File;
import java.lang.reflect.Field;

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
		return "net.sparktank.morrigan.playbackimpl.dsj version 0.01.";
	}
	
	@Override
	public void setClassPath(File[] classPath) {
		this.classPath = classPath;
	}
	
	@Override
	public void registerHotkey(int id, int key, boolean ctrl, boolean shift, boolean alt, boolean supr) throws HotkeyException {
		shoeHorn();
		
		// TODO
		
	}
	
	@Override
	public void unregisterHotkey(int id) throws HotkeyException {
		
		// TODO
		
	}
	
	@Override
	public void setListener(IHotkeyListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
		
		File dsjDllFile = null;
		
		for (File classPathFile : classPath) {
			if (classPathFile.isDirectory()) {
				File[] listFiles = classPathFile.listFiles();
				if (listFiles!=null && listFiles.length>0) {
					for (File file : listFiles) {
						if (file.isFile()) {
							if (file.getName().equals(dllName)) {
								dsjDllFile = file;
								break;
							}
						}
					}
				}
			}
		}
		
		if (dsjDllFile==null) return;
		System.out.println(dllName + "=" + dsjDllFile.getAbsolutePath());
		
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible) field.setAccessible(true);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.setProperty("java.library.path", dsjDllFile.getParentFile().getAbsolutePath());
		
		/* FIXME
		 * This next line fails with
		 * java.lang.UnsatisfiedLinkError: Native Library D:\haku\development\eclipseWorkspace-java\dsjtest\lib\dsj.dll already loaded in another classloader
		 * if it is already loaded.
		 */
		try {
			System.load(dsjDllFile.getAbsolutePath());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		haveShoeHorned = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package com.vaguehope.morrigan.engines;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("boxing")
public class HotkeyKeys {
	
	public static final Map<Integer, String> HkKeys = new HashMap<Integer, String>() {
		
		private static final long serialVersionUID = 2399324929460590297L;
		
		private void put(int i, char c) {
			put(i, Character.toString(c));
		}
		
		private void put(char c, String name) {
			put((int)c, name);
		}
		
		{
			for (int i = 'A'; i <= 'Z'; i++) {
				put(i, (char) i);
			}
			
			for (int i = '0'; i <= '9'; i++) {
				put(i, (char) i);
			}
			
			put (' ', "Space");
			
			put (0x70, "F1");
			put (0x71, "F2");
			put (0x72, "F3");
			put (0x73, "F4");
			put (0x74, "F5");
			put (0x75, "F6");
			put (0x76, "F7");
			put (0x77, "F8");
			put (0x78, "F9");
			put (0x79, "F10");
			put (0x7A, "F11");
			put (0x7B, "F12");
			
			put (0x26, "Up");
			put (0x28, "Down");
			put (0x25, "Left");
			put (0x27, "Right");
			put (0x24, "Home");
			put (0x23, "End");
			put (0x21, "Page up");
			put (0x22, "Page down");
			put (0x9B, "Insert");
			put (0x9A, "Print Screen");
			
			put (0x2F, "/");
			put (0x5C, "\\");
			
		}
	};
	
}

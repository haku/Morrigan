package net.sparktank.morrigan.engines.hotkey;

public class HotkeyValue {
	
	private final int key;
	private final boolean ctrl;
	private final boolean shift;
	private final boolean alt;
	private final boolean supr;
	
	public HotkeyValue (int key, boolean ctrl, boolean shift, boolean alt, boolean supr) {
		this.key = key;
		this.ctrl = ctrl;
		this.shift = shift;
		this.alt = alt;
		this.supr = supr;
	}
	
	public HotkeyValue (String serialised) {
		String[] arr = serialised.split("\\|");
		
		if (arr.length != 5) {
			throw new IllegalArgumentException("Not a hotkey serial string: '" + serialised + "'.");
		}
		
		this.key = Integer.valueOf(arr[0]);
		this.ctrl = intToBoolean(Integer.valueOf(arr[1]));
		this.shift = intToBoolean(Integer.valueOf(arr[2]));
		this.alt = intToBoolean(Integer.valueOf(arr[3]));
		this.supr = intToBoolean(Integer.valueOf(arr[4]));
	}
	
	public int getKey() { return key; }
	public boolean getCtrl () { return ctrl; }
	public boolean getShift () { return shift; }
	public boolean getAlt () { return alt; }
	public boolean getSupr () { return supr; }
	
	public String serialise () {
		return key
			+ "|" + booleanToInt(ctrl)
			+ "|" + booleanToInt(shift)
			+ "|" + booleanToInt(alt)
			+ "|" + booleanToInt(supr);
	}
	
	private int booleanToInt (boolean b) {
		if (b) { return 1; } else { return 0; } 
	}
	
	private boolean intToBoolean (int i) {
		if (i==1) { return true; } else { return false; } 
	}
	
	@Override
	public String toString() {
		return "[" + key + "," + ctrl + "," + shift + "," + alt + "," + supr + "]";
	}
	
}

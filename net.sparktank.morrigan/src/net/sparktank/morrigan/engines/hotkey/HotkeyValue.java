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
	
	@SuppressWarnings("boxing")
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
	
	public int getKey() { return this.key; }
	public boolean getCtrl () { return this.ctrl; }
	public boolean getShift () { return this.shift; }
	public boolean getAlt () { return this.alt; }
	public boolean getSupr () { return this.supr; }
	
	public String serialise () {
		return this.key
			+ "|" + booleanToInt(this.ctrl)
			+ "|" + booleanToInt(this.shift)
			+ "|" + booleanToInt(this.alt)
			+ "|" + booleanToInt(this.supr);
	}
	
	private int booleanToInt (boolean b) {
		if (b) { return 1; }
		
		return 0; 
	}
	
	private boolean intToBoolean (int i) {
		if (i==1) { return true; }
		
		return false; 
	}
	
	@Override
	public String toString() {
		return "[" + this.key + "," + this.ctrl + "," + this.shift + "," + this.alt + "," + this.supr + "]";
	}
	
}

package net.sparktank.morrigan.model.explorer;

public class MenuItem {
	
	public String identifier;
	public String title;
	
	public MenuItem () {
	}
	
	public MenuItem (String name, String title) {
		this.identifier = name;
		this.title = title;
	}
	
	@Override
	public String toString () {
		return title;
	}
	
}

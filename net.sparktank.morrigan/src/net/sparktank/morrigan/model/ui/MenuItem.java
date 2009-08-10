package net.sparktank.morrigan.model.ui;

public class MenuItem {
	
	public String identifier;
	public String title;
	
	public MenuItem () {
	}
	
	public MenuItem (String name, String title) {
		this.identifier = name;
		this.title = title;
	}
	
	public String toString () {
		return title;
	}
	
}

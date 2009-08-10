package net.sparktank.morrigan.model;

public class MenuItem {
	
	public String name;
	public String title;
	
	public MenuItem (String name, String title) {
		this.name = name;
		this.title = title;
	}
	
	public String toString () {
		return title;
	}
	
}

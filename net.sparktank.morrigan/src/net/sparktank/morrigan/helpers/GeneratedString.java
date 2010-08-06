package net.sparktank.morrigan.helpers;

public class GeneratedString {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String string = null;
	
	public GeneratedString () {
		this.string = generateString();
	}
	
	public String generateString () {
		throw new RuntimeException("generateString() not implemented.");
	}
	
	@Override
	public String toString () {
		return this.string;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

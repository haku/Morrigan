package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.helper.XmlParser;
import net.sparktank.morrigan.android.model.MlistState;

import org.xml.sax.SAXException;

public class MlistStateXmlImpl  extends XmlParser implements MlistState {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TITLE = "title";
	public static final String COUNT = "count";
	public static final String DURATION = "duration";
	public static final String DURATIONCOMPLETE = "durationcomplete";
	
	public final static String[] nodes = new String[] { 
		TITLE,
		COUNT,
		DURATION,
		DURATIONCOMPLETE
		};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MlistStateXmlImpl (String xmlString) throws SAXException {
		super(xmlString, nodes);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public int getImageResource() {
		return R.drawable.db;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return this.getNode(TITLE);
	}
	
	@Override
	public int getCount() {
		return this.getNodeInt(COUNT);
	}
	
	@Override
	public long getDuration() {
		return this.getNodeLong(DURATION);
	}
	
	@Override
	public boolean isDurationComplete() {
		return this.getNodeBoolean(DURATIONCOMPLETE);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

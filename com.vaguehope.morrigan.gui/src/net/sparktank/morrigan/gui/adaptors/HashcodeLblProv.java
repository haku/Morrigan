package net.sparktank.morrigan.gui.adaptors;

import java.math.BigInteger;


import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.vaguehope.morrigan.model.media.IMediaItem;

public class HashcodeLblProv extends ColumnLabelProvider {
	
	public HashcodeLblProv () {/* UNUSED */}
	
	@Override
	public String getText(Object element) {
		IMediaItem elm = (IMediaItem) element;
		if (elm.getHashcode() == null || elm.getHashcode().equals(BigInteger.ZERO)) {
			return null;
		}
		
		return elm.getHashcode().toString(16);
	}
	
}
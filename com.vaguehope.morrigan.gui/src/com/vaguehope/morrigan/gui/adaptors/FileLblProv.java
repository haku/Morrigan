package com.vaguehope.morrigan.gui.adaptors;

import java.math.BigInteger;


import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.TextStyle;

import com.vaguehope.morrigan.gui.helpers.ImageCache;
import com.vaguehope.morrigan.model.media.IMediaItem;

public class FileLblProv extends StyledCellLabelProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String MSG_DEC_MISSING = " (missing)";
	private static final String MSG_DEC_DISABLED = " (disabled)";
	
	private static final Styler STRIKEOUT_ITEM_STYPE = new Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.strikeout = true;
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ImageCache imageCache;
	
	public FileLblProv (ImageCache imageCache) {
		this.imageCache = imageCache;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof IMediaItem) {
			IMediaItem mi = (IMediaItem) element;
			
			if (mi.getTitle() != null) {
				Styler styler = null;
				if (mi.isMissing() || !mi.isEnabled()) {
					styler = FileLblProv.STRIKEOUT_ITEM_STYPE;
				}
				StyledString styledString = new StyledString(mi.getTitle(), styler);
				
				String dec = null;
				if (mi.isMissing()) {
					dec = MSG_DEC_MISSING;
				} else if (!mi.isEnabled()) {
					dec = MSG_DEC_DISABLED;
				}
				
				if (dec != null) {
					styledString.append(dec, StyledString.DECORATIONS_STYLER);
				}
				
				cell.setText(styledString.toString());
				cell.setStyleRanges(styledString.getStyleRanges());
				
			} else {
				cell.setText(null);
			}
			
			if (mi.isMissing()) {
				cell.setImage(null); // TODO find icon for missing?
			} else if (!mi.isEnabled()) {
				cell.setImage(this.imageCache.readImage("icons/noentry-red.png"));
			} else if (mi.getHashcode() == null || mi.getHashcode().equals(BigInteger.ZERO)) {
				cell.setImage(this.imageCache.readImage("icons/exclamation-red.png"));
			} else {
				cell.setImage(this.imageCache.readImage("icons/circledot.png"));
			}
			
		}
		super.update(cell);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
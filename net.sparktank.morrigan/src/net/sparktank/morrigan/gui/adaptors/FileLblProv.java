package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.MediaItem;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.TextStyle;

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
		if (element instanceof MediaItem) {
			MediaItem mi = (MediaItem) element;
			
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
				cell.setImage(null); // TODO find icon for disabled?
			} else {
				cell.setImage(this.imageCache.readImage("icons/playlist.gif")); // TODO find icon for items?
			}
			
		}
		super.update(cell);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
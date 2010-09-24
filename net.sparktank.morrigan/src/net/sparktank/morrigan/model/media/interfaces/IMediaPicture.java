package net.sparktank.morrigan.model.media.interfaces;

public interface IMediaPicture extends IMediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isPicture ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getWidth();
	public boolean setWidth(int width);
	
	public int getHeight();
	public boolean setHeight(int height);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean setFromMediaPicture (IMediaPicture mp);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

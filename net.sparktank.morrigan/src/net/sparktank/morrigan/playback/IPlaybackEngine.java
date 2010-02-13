package net.sparktank.morrigan.playback;

import java.awt.Frame;

public interface IPlaybackEngine {
	
	public enum PlayState { Stopped, Playing, Paused, Loading };
	
	/**
	 * Returns the description of this playback engine.
	 */
	public String getAbout ();
	
	/**
	 * Returns a list of the file extensions that can be played
	 * when this engine is loaded.
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 */
	public String[] getSupportedFormats ();
	
	/**
	 * Set the file to play.  Weather it is actually
	 * loaded here or when needed is implementation specific.
	 * Will stop playback if needed.
	 * @param filepath
	 */
	public void setFile (String filepath);
	
	/**
	 * Set the control where the output video will be shown.
	 * @param frame a java.awt.Frame object.
	 */
	public void setVideoFrame (Frame frame);
	
	/**
	 * Completly free the current file.
	 * Will stop playback if needed.
	 * This function may be already be covered by
	 * <code>stopPlaying()</code>.
	 */
	public void unloadFile ();
	
	/**
	 * This should be called before discarding
	 * the reference to this implementation.
	 */
	public void finalise ();
	
	/**
	 * Begin playback.
	 */
	public void startPlaying () throws PlaybackException;
	
	/**
	 * Stop playback.
	 */
	public void stopPlaying () throws PlaybackException;
	
	/**
	 * Pause playback.  It should be safe to call this even
	 * if the track is already paused.
	 */
	public void pausePlaying () throws PlaybackException;
	
	/**
	 * Resume paused playback.  It should be safe to call this
	 * even if the track is not paused. 
	 */
	public void resumePlaying () throws PlaybackException;
	
	/**
	 * Returns the current play state.
	 * @return
	 */
	public PlayState getPlaybackState ();
	
	/**
	 * Returns the duration of the current file.
	 * @return Duration in seconds.  Returns -1 if not implmented.
	 */
	public int getDuration () throws PlaybackException;
	
	/**
	 * The position the current file.
	 * @return Position in seconds.
	 */
	public long getPlaybackProgress () throws PlaybackException;
	
	/**
	 * The methods in this class will be called when their event occures.
	 * @param listener
	 */
	public void setStatusListener (IPlaybackStatusListener listener);
	
}

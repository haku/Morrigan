package com.vaguehope.morrigan.engines.playback;

import java.io.File;

import org.eclipse.swt.widgets.Composite;

public interface IPlaybackEngine {
	
	public enum PlayState {
		Stopped(0), Playing(1), Paused(2), Loading(3);
		
		private int n;
		
		private PlayState (int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
		}
		
	}
	
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
	 * Helper method for quickly reading duration from a file.
	 * @param filepath Media file to interegate.
	 * @return duration of file in seconds.
	 */
	public int readFileDuration (String filepath) throws PlaybackException;
	
	/**
	 * This method will be called by the plugin loader shortly after
	 * the instance of the plugin is created.
	 * @param classPath The array of File objects used by the
	 * classloader to load the plugin.
	 */
	public void setClassPath (File[] classPath);
	
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
	public void setVideoFrameParent (Composite frame);
	
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
	 * Load track.  Must be called before startPlaying().
	 * This may be called by a non-UI thread.
	 */
	public void loadTrack () throws PlaybackException;
	
	/**
	 * Begin playback.
	 * This may be called by a non-UI thread.
	 * If videoFrameParent is set then this method may
	 * use the Display from videoFrameParent to run
	 * work on the UI thread.
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
	 * Seek to a specific position in the track.
	 * @param d a double where 0 <= d <= 1.
	 * @throws PlaybackException
	 */
	public void seekTo (double d) throws PlaybackException;
	
	/**
	 * The methods in this class will be called when their event occures.
	 * @param listener
	 */
	public void setStatusListener (IPlaybackStatusListener listener);
	
}

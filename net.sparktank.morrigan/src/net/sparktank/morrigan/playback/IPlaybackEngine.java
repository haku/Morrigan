package net.sparktank.morrigan.playback;

public interface IPlaybackEngine {
	
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
	 * Pause playback.
	 */
	public void pausePlaying () throws PlaybackException;
	
	/**
	 * Returns the duration of the current file.
	 * @return Duration in seconds.  Returns -1 if not implmented.
	 */
	public int getDuration () throws PlaybackException;
	
	/**
	 * The progress playing the current file.
	 * @return Percentage progress p where 0 <= p <= 100.  Returns -1 if not implmented.
	 */
	public int getPlaybackProgress () throws PlaybackException;
	
	/**
	 * This runnable will be executed when the end of the file is reached.
	 * @param runnable
	 */
	public void setOnfinishHandler (Runnable runnable);
	
}

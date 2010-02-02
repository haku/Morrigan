package net.sparktank.morrigan.playback;

public interface IPlaybackEngine {
	
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
	public void startPlaying ();
	
	/**
	 * Stop playback.
	 */
	public void stopPlaying ();
	
	/**
	 * Pause playback.
	 */
	public void pausePlaying ();
	
	/**
	 * Returns the duration of the current file.
	 * @return Duration in seconds.
	 */
	public int getDuration ();
	
	/**
	 * The progress playing the current file.
	 * @return Percentage progress p where 0 <= p <= 100.
	 */
	public int getPlaybackProgress ();
	
	/**
	 * This runnable will be executed when the end of the file is reached.
	 * @param runnable
	 */
	public void setOnfinishHandler (Runnable runnable);
	
}

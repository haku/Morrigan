package com.vaguehope.morrigan.playbackimpl.dsj;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;


import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackException;

import de.humatic.dsj.DSEnvironment;
import de.humatic.dsj.DSFiltergraph;
import de.humatic.dsj.DSJUtils;
import de.humatic.dsj.DSMediaType;
import de.humatic.dsj.DSMovie;

/* Main play-back class:
 * Java doc:
 * http://www.humatic.de/htools/dsj/javadoc/index.html
 * 
 * http://www.humatic.de/htools/dsj/javadoc/de/humatic/dsj/DSFiltergraph.html
 * 
 * Event constants:
 * http://www.humatic.de/htools/dsj/javadoc/constant-values.html
 */
public class PlaybackEngine implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO Any more?
	private static final String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	protected volatile boolean m_stopPlaying;
	
	private String filepath = null;
	protected Composite videoFrameParent = null;
	private IPlaybackStatusListener listener = null;
	private PlayState playbackState = PlayState.STOPPED;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public PlaybackEngine () {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.
	
	@Override
	public String getAbout () {
		return "com.vaguehope.morrigan.playbackimpl.dsj version 0.01.";
	}
	
	@Override
	public String[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	@Override
	public int readFileDuration(String file) throws PlaybackException {
		int[] stats = DSJUtils.getBasicFileStats(file);
		return stats[0] / 1000;
	}
	
	@Override
	public void setClassPath(File[] classPath) {
		// Unused.
	}
	
	@Override
	public void setFile(String filepath) {
		this.filepath = filepath;
	}
	
	@Override
	public void setVideoFrameParent(Composite frame) {
		if (frame==this.videoFrameParent) return;
		this.videoFrameParent = frame;
		_reparentVideo();
	}
	
	@Override
	public void unloadFile() {
		_finalisePlayback();
	}
	
	@Override
	public void finalise() {/* UNUSED */}

	@Override
	public void loadTrack() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		_loadTrack();
		long l0 = System.currentTimeMillis() - t0;
		this.logger.fine("Track load time: "+l0+" ms.");
	}
	
	@Override
	public void startPlaying() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		_startTrack();
		long l0 = System.currentTimeMillis() - t0;
		this.logger.fine("Track start time: "+l0+" ms.");
	}
	
	@Override
	public void stopPlaying() throws PlaybackException {
		_stopTrack();
	}
	
	@Override
	public void pausePlaying() throws PlaybackException {
		_pauseTrack();
	}
	
	@Override
	public void resumePlaying() throws PlaybackException {
		_resumeTrack();
	}
	
	@Override
	public PlayState getPlaybackState() {
		return this.playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		if (this.dsMovie!=null) {
			return this.dsMovie.getDuration() / 1000;
		}
		return -1;
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		if (this.dsMovie!=null) {
			return this.dsMovie.getTime() / 1000;
		}
		return -1;
	}
	
	@Override
	public void seekTo(double d) throws PlaybackException {
		if (this.dsMovie!=null) {
			int duration = this.dsMovie.getDuration();
			if (duration > 0) {
				this.dsMovie.setTimeValue((int) (d * duration));
			}
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	protected DSMovie dsMovie = null;
	protected Composite videoComposite = null;   // SWT.
	protected Frame videoFrame = null;           // AWT.
	protected Component videoComponent = null;   // AWT.
	
	private void _finalisePlayback () {
		if (this.dsMovie!=null) {
			_stopTrack();
			
			if (this.videoComponent!=null) {
				this.videoFrame.remove(this.videoComponent);
				this.videoFrame.dispose();
				this.videoFrame = null;
				
				this.videoComponent.invalidate();
				this.videoComponent = null;
				
				if (!this.videoComposite.isDisposed()) {
					if (this.videoComposite.getDisplay().getThread().equals(Thread.currentThread())) {
						this.videoComposite.dispose();
					} else {
						this.videoComposite.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								PlaybackEngine.this.videoComposite.dispose();
							}
						});
					}
				}
				this.videoComposite = null;
			}
			
			this.dsMovie.dispose();
			this.dsMovie = null;
		}
		
		_setScreenSaverActive(true);
	}
	
	private void _loadTrack () throws PlaybackException {
		try {
			_callStateListener(PlayState.LOADING);
			boolean firstLoad = (this.dsMovie==null);
			
			this.logger.fine("dsj.PlaybackEngine firstLoad=" + firstLoad);
			
			if (firstLoad) {
//				_shoeHorn();
			} else {
				_finalisePlayback();
			}
			
			_setScreenSaverActive(false); // FIXME THIS IS A WORKAROUND!
			
			DSEnvironment.setDebugLevel(3); // 0 to 3.  3 is max.
			/* error codes can be looked up in de.humatic.dsj.DSJException
			 * Also error codes are in:
			 * http://www.humatic.de/htools/dsj/javadoc/constant-values.html
			 */
			this.dsMovie = new DSMovie(this.filepath,
					DSFiltergraph.OVERLAY | DSFiltergraph.MOUSE_ENABLED | DSFiltergraph.INIT_PAUSED,
					this.propertyChangeLlistener);
			this.dsMovie.setVolume(1.0f);
			this.dsMovie.setRecueOnStop(false);
			
			_reparentVideo();
		}
		catch (Exception e) {
			_callStateListener(PlayState.STOPPED);
			throw new PlaybackException("Failed to load '"+this.filepath+"'.", e);
		}
	}
	
	private void _startTrack () {
		this.logger.fine("dsj.PlaybackEngine playTrack()");
		
		this.m_stopPlaying = false;
		
		if (this.dsMovie!=null) {
			/* I wish I knew why this made a difference...
			 * Without doing it like this, it sometimes play video
			 * at high speed and with no sound.
			 * This makes no sense to me.
			 */
			if (this.videoFrameParent != null) {
				this.videoFrameParent.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						PlaybackEngine.this.dsMovie.pause(); // If this is not here, then sometimes playback does not start when play() is called.
						PlaybackEngine.this.dsMovie.play();
					}
				});
			}
			else {
				this.dsMovie.play();
			}
			
			_startWatcherThread();
			_callStateListener(PlayState.PLAYING);
		}
	}
	
	private void _pauseTrack () {
		if (this.dsMovie!=null) {
			this.dsMovie.pause();
			_callStateListener(PlayState.PAUSED);
			_setScreenSaverActive(true);
		}
	}
	
	private void _resumeTrack () {
		if (this.dsMovie!=null) {
			_setScreenSaverActive(false);
			this.dsMovie.play();
			_callStateListener(PlayState.PLAYING);
		}
	}
	
	private void _stopTrack () {
		this.m_stopPlaying = true;
		_stopWatcherThread();
		if (this.dsMovie!=null) {
			this.dsMovie.stop();
			_callStateListener(PlayState.STOPPED);
			_setScreenSaverActive(true);
		}
	}
	
	private void _reparentVideo () {
		this.logger.fine("dsj.PlaybackEngine reparentVideo()");
		
//		if (videoComponent!=null) {
//			logger.fine("remove listeners");
//			videoComponent.removeMouseListener(mouseListener);
//			videoComponent.removeKeyListener(keyListener);
//		}
		
//		if (videoFrame!=null) {
//			if (videoComponent!=null) videoFrame.remove(videoComponent);
//			videoFrame.dispose();
//			videoFrame = null;
//		}
		
//		if (videoComposite!=null) {
//			videoComposite.dispose();
//			videoComposite = null;
//		}
		
		if (this.videoFrameParent==null) return;
		if (this.dsMovie==null) return;
		if (!this.dsMovie.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) return;
		
		this.videoFrameParent.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (PlaybackEngine.this.videoComponent==null) {
						PlaybackEngine.this.videoComponent = PlaybackEngine.this.dsMovie.asComponent();
						PlaybackEngine.this.videoComponent.setBackground(Color.BLACK);
						
						PlaybackEngine.this.logger.fine("dsj.PlaybackEngine Adding listeners to videoComponent...");
						PlaybackEngine.this.videoComponent.addMouseListener(PlaybackEngine.this.mouseListener);
						PlaybackEngine.this.videoComponent.addKeyListener(PlaybackEngine.this.keyListener);
					}
					
					if (PlaybackEngine.this.videoComposite == null || PlaybackEngine.this.videoComposite.isDisposed()) {
						PlaybackEngine.this.logger.fine("dsj.PlaybackEngine Making videoComposite...");
						
						PlaybackEngine.this.videoComposite = new Composite(PlaybackEngine.this.videoFrameParent, SWT.EMBEDDED);
						PlaybackEngine.this.videoComposite.setLayout(new FillLayout());
						
						PlaybackEngine.this.videoFrame = SWT_AWT.new_Frame(PlaybackEngine.this.videoComposite);
						PlaybackEngine.this.videoFrame.setBackground(Color.BLACK);
						
						PlaybackEngine.this.videoFrame.add(PlaybackEngine.this.videoComponent, BorderLayout.CENTER);
						PlaybackEngine.this.videoFrame.doLayout();
						
					} else {
						PlaybackEngine.this.logger.fine("dsj.PlaybackEngine Moveing videoComposite...");
						PlaybackEngine.this.videoComposite.setParent(PlaybackEngine.this.videoFrameParent);
					}
					
					PlaybackEngine.this.videoFrameParent.layout();
				}
				catch (Exception e) {
					callOnErrorHandler(e);
				}
			}
		});
		
		this.logger.fine("dsj.PlaybackEngine Leaving reparentVideo()");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Listeners.
	
	protected MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			_callOnClickListener(e.getButton(), e.getClickCount());
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {/* UNUSED */}
		@Override
		public void mousePressed(MouseEvent e) {/* UNUSED */}
		@Override
		public void mouseExited(MouseEvent e) {/* UNUSED */}
		@Override
		public void mouseEntered(MouseEvent e) {/* UNUSED */}
	};
	
	protected KeyListener keyListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent key) {
			_callOnKeyPressListener(key.getKeyCode());
		}
		@Override
		public void keyReleased(KeyEvent key) {/* UNUSED */}
		@Override
		public void keyPressed(KeyEvent key) {/* UNUSED */}
	};
	
	private PropertyChangeListener propertyChangeLlistener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent pce) {
			switch (DSJUtils.getEventType(pce)) {
				
//				case (AVCDevice.ED_MODE_PLAY):
//					callStateListener(PlayState.Playing);
//					break;
				
//				case (AVCDevice.ED_MODE_STOP):
//					callStateListener(PlayState.Stopped);
//					break;
					
				case (DSFiltergraph.DONE):
					if (!PlaybackEngine.this.m_stopPlaying) {
						_callOnEndOfTrackHandler();
					} else {
						PlaybackEngine.this.logger.fine("dsj.PlaybackEngine Not calling onEndOfTrackHandler because we are stopping.");
					}
					break;
				
//				case (DSFiltergraph.BUFFERING):
//					logger.fine("dsj.PlaybackEngine DSFiltergraph.BUFFERING"); 
//					break;
				
//				case (DSFiltergraph.BUFFER_COMPLETE):
//					logger.fine("dsj.PlaybackEngine DSFiltergraph.BUFFER_COMPLETE");
//					break;
					
//				case (DSFiltergraph.INITIALIZED):
//					logger.fine("dsj.PlaybackEngine DSFiltergraph.INITIALIZED");
//					break;
					
//				case (DSFiltergraph.IP_READY):
//					logger.fine("dsj.PlaybackEngine DSFiltergraph.IP_READY");
//					break;
					
//				case (DSFiltergraph.OVERLAY_BUFFER_REQUEST):
//					logger.fine("dsj.PlaybackEngine DSFiltergraph.OVERLAY_BUFFER_REQUEST");
//					break;
					
//				case (DSFiltergraph.GRAPH_ERROR):
//					String message = "Graph error: " + String.valueOf(pce.getNewValue());
//					callOnErrorHandler(new PlaybackException(message));
//					break;
				
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Watcher thread.
	
	protected volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	private void _startWatcherThread () {
		this.m_stopWatching = false;
		this.watcherThread = new WatcherThread();
		this.watcherThread.setDaemon(true);
		this.watcherThread.start();
	}
	
	private void _stopWatcherThread () {
		this.m_stopWatching = true;
		try {
			if (this.watcherThread!=null
					&& this.watcherThread.isAlive()
					&& !Thread.currentThread().equals(this.watcherThread)) {
				this.watcherThread.join();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class WatcherThread extends Thread {
		
		private int lastMeasuredPosition = -1;
		private int lastSentPosition = -1;
		
		public WatcherThread() {/* UNUSED */}
		
		@Override
		public void run() {
			while (!PlaybackEngine.this.m_stopWatching) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {/* UNUSED */}
				
				if (PlaybackEngine.this.dsMovie!=null) {
					int measuredPosition = PlaybackEngine.this.dsMovie.getTime();
					if (getPlaybackState() == PlayState.PLAYING && measuredPosition == this.lastMeasuredPosition) {
						PlaybackEngine.this.logger.fine("dsj.PlaybackEngine Prompting playback...");
						PlaybackEngine.this.dsMovie.play();
					}
					this.lastMeasuredPosition = measuredPosition;
					
					int position = measuredPosition / 1000;
					if (position != this.lastSentPosition) {
						_callPositionListener(position);
						this.lastSentPosition = position;
					}
				}
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	protected void callOnErrorHandler (Exception e) {
		if (this.listener!=null) {
			this.listener.onError(e);
		}
	}
	
	protected  void _callOnEndOfTrackHandler () {
		_callStateListener(PlayState.STOPPED);
		if (this.listener!=null) {
			this.listener.onEndOfTrack();
		}
	}
	
	private void _callStateListener (PlayState state) {
		this.playbackState = state;
		if (this.listener!=null) this.listener.statusChanged(state);
	}
	
	protected void _callPositionListener (long position) {
		if (this.listener!=null) {
			this.listener.positionChanged(position);
		}
	}
	
	protected void _callOnKeyPressListener (int keyCode) {
		if (this.listener!=null) {
			this.listener.onKeyPress(keyCode);
		}
	}
	
	protected void _callOnClickListener (int button, int clickCount) {
		if (this.listener!=null) {
			this.listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Screen saver.
	
	private void _setScreenSaverActive (boolean active) {
		if (DSJUtils.getScreenSaverActive()!=active) {
			if (active || (this.dsMovie!=null && this.dsMovie.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) ) {
				
				DSJUtils.setScreenSaverActive(active); // FIXME crashes JVM ???
				
				boolean a = DSJUtils.getScreenSaverActive();
				if (active == a) {
					this.logger.fine("dsj.PlaybackEngine Set screenSaverActive=" + active + ".");
				} else {
					this.logger.fine("dsj.PlaybackEngine Failed to set screenSaverActive=" + active + ".");
				}
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.playbackimpl.gs;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.gstreamer.Bus;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Format;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.elements.PlayBin;
import org.gstreamer.swt.overlay.VideoComponent;

/* Main playback class:
 * http://www.humatic.de/htools/dsj/javadoc/de/humatic/dsj/DSFiltergraph.html
 * 
 * Event constants:
 * http://www.humatic.de/htools/dsj/javadoc/constant-values.html
 */

public class PlaybackEngine implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constants.
	
	// TODO Any more?
	private final static String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
	private static final int FILE_READ_DURATION_TIMEOUT = 5000; // 5 seconds.
	private static final int WAIT_FOR_decodeElement_TIMEOUT = 30000; // 30 seconds.
	private static final int WATCHER_POLL_INTERVAL_MILLIS = 500; // 0.5 seconds.
	private static final int EOS_MAN_LIMIT = 10; // 10*500 = 5 seconds.
	private static final int EOS_MARGIN_SECONDS = 1; // 1 second from end of track.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final AtomicBoolean m_atEos = new AtomicBoolean();
	private final AtomicBoolean m_stopPlaying = new AtomicBoolean();
	
	private String filepath = null;
	Composite videoFrameParent = null;
	private IPlaybackStatusListener listener = null;
	PlayState playbackState = PlayState.Stopped;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.

	public PlaybackEngine () { /* UNUSED */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.
	
	@Override
	public String getAbout () {
		return "net.sparktank.morrigan.playbackimpl.gs version 0.01.";
	}
	
	@Override
	public String[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	@Override
	public int readFileDuration(final String fpath) throws PlaybackException {
		PlayBin playb = new PlayBin("Metadata");
//		playb.setVideoSink(ElementFactory.make("fakesink", "videosink"));
		playb.setVideoSink(null);
		playb.setInputFile(new File(fpath));
		playb.setState(State.PAUSED);
		
		long queryDuration = -1;
		long startTime = System.currentTimeMillis();
		while (true) {
			queryDuration = playb.queryDuration(TimeUnit.MILLISECONDS);
			if (queryDuration > 0 || System.currentTimeMillis() - startTime > FILE_READ_DURATION_TIMEOUT) {
				break;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) { /* UNUSED */ }
		}
		playb.setState(State.NULL);
		playb.dispose();
		
		int retDuration = -1;
		if (queryDuration > 0) {
			retDuration = (int) (queryDuration / 1000);
			if (retDuration < 1) retDuration = 1;
		}
		
		return retDuration;
	}
	
	@Override
	public void setClassPath(File[] arg0) { /* UNUSED */ }
	
	@Override
	public void setFile(String filepath) {
		this.filepath = filepath;
	}
	
	@Override
	public void setVideoFrameParent (Composite frame) {
		if (frame == this.videoFrameParent) return;
		this.videoFrameParent = frame;
		reparentVideo();
	}
	
	@Override
	public void unloadFile() {
		finalisePlayback();
	}
	
	@Override
	public void finalise() {
		finalisePlayback();
	}
	
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
		this.m_stopPlaying.set(true);
		stopTrack();
	}
	
	@Override
	public void pausePlaying() throws PlaybackException {
		pauseTrack();
	}
	
	@Override
	public void resumePlaying() throws PlaybackException {
		resumeTrack();
	}
	
	@Override
	public PlayState getPlaybackState() {
		return this.playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		return (int) this.playbin.queryDuration(TimeUnit.SECONDS);
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		return this.playbin.queryPosition(TimeUnit.SECONDS);
	}
	
	@Override
	public void seekTo(double d) throws PlaybackException {
		this.playLock.lock();
		try {
			if (this.playbin!=null) {
				long duration = this.playbin.queryDuration(TimeUnit.NANOSECONDS);
				this.playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, (long) (d * duration), SeekType.NONE, -1);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	private Lock playLock = new ReentrantLock();
	PlayBin playbin = null;
	VideoComponent videoComponent = null;   // SWT / GStreamer.
	Element videoElement = null;            // GStreamer.
	volatile boolean hasVideo = false;
	
	private void finalisePlayback () {
		this.logger.entering(this.getClass().getName(), "finalisePlayback");
		
		stopWatcherThread();
		
		this.playLock.lock();
		try {
			if (this.playbin!=null) {
				this.playbin.setState(State.NULL);
				this.playbin.dispose();
				this.playbin = null;
				if (this.videoElement != null) this.videoElement.dispose();
				
				if (this.videoComponent!=null) {
					if (!this.videoComponent.isDisposed()) {
						_runInUiThread(this.videoComponent, new Runnable() {
							@Override
							public void run() {
								PlaybackEngine.this.videoComponent.removeKeyListener(PlaybackEngine.this.keyListener);
								PlaybackEngine.this.videoComponent.removeMouseListener(PlaybackEngine.this.mouseListener);
								PlaybackEngine.this.videoComponent.dispose();
							}
						});
					}
					this.videoComponent = null;
					
					if (this.videoFrameParent!=null && !this.videoFrameParent.isDisposed()) {
						_runInUiThread(this.videoFrameParent, new Runnable() {
							@Override
							public void run() {
								if (!PlaybackEngine.this.videoFrameParent.isDisposed()) {
									PlaybackEngine.this.videoFrameParent.redraw();
								}
							}
						});
					}
				}
			}
		}
		finally {
			this.playLock.unlock();
		}
		
		this.logger.exiting(this.getClass().getName(), "finalisePlayback");
	}
	
	private void _loadTrack () throws PlaybackException {
		this.logger.entering(this.getClass().getName(), "_loadTrack");
		
		this.playLock.lock();
		try {
			callStateListener(PlayState.Loading);
			boolean firstLoad = (this.playbin==null);
			
			this.logger.fine("firstLoad=" + firstLoad);
			
			if (firstLoad) {
				this.logger.fine("About to create PlayBin object...");
				this.playbin = new PlayBin("VideoPlayer");
				
				this.logger.fine("Connecting eosBus...");
				this.playbin.getBus().connect(this.eosBus);
				this.logger.fine("Connecting stateChangedBus...");
				this.playbin.getBus().connect(this.stateChangedBus);
				
			} else {
				this.playbin.setState(State.NULL);
			}
			
			if (this.videoFrameParent == null) {
				this.hasVideo = false;
			}
			else {
				this.hasVideo = mightFileHaveVideo(this.filepath);
			}
			
			this.logger.fine("About to set input file to '"+this.filepath+"'...");
	        this.playbin.setInputFile(new File(this.filepath));
	        this.logger.fine("Set file input file.");
	        
        	reparentVideo(false);
		}
		catch (Throwable t) {
			callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+this.filepath+"'.", t);
		}
		finally {
			this.playLock.unlock();
		}
		
		this.logger.exiting(this.getClass().getName(), "_loadTrack");
	}
	
	void reparentVideo () {
		reparentVideo(true);
	}
	
	@SuppressWarnings("boxing")
	private void reparentVideo (boolean seek) {
		this.logger.entering(this.getClass().getName(), "reparentVideo", seek);
		
		// TODO tidy this.
		if (this.videoComponent != null) {
			_runInUiThread(this.videoComponent, new Runnable() {
				@Override
				public void run() {
					if (PlaybackEngine.this.videoComponent.getParent() == PlaybackEngine.this.videoFrameParent) {
						PlaybackEngine.this.logger.warning("Reparenting to same parent!");
					}
				}
			});
		}
		
		this.playLock.lock();
		try {
			// Clean up any stuff we had before.
			if (this.videoComponent != null) {
				if (!this.videoComponent.isDisposed()) {
					this.logger.fine("Removing listeners...");
					_runInUiThread(this.videoComponent, new Runnable() {
						@Override
						public void run() {
							PlaybackEngine.this.videoComponent.removeKeyListener(PlaybackEngine.this.keyListener);
							PlaybackEngine.this.videoComponent.removeMouseListener(PlaybackEngine.this.mouseListener);
							PlaybackEngine.this.logger.fine("listeners removed.");
						}
					});
				}
			}
			
			// Clear old values but keep references so we can dispose them later.
			final VideoComponent old_videoComponent = this.videoComponent;
			this.videoComponent = null;
			Element old_videoElement = this.videoElement;
			this.videoElement = null;
			
			// Do we have anything to attach video output to?
			if (this.playbin != null) {
				// Can not attach video to something that is not there...
				if (this.videoFrameParent != null && this.hasVideo) {
					/*
					 * We can not move the video while it is playing, so if it is,
					 * stop it and remember where it was.
					 */
					long position = -1;
					this.logger.fine("checking current state...");
					State state = this.playbin.getState();
					this.logger.fine("state = " + state + ".");
					if (state == State.PLAYING || state == State.PAUSED) {
						this.logger.fine("stopping playback...");
						position = this.playbin.queryPosition(TimeUnit.NANOSECONDS);
						this.logger.fine("position=" + position);
						this.playbin.setState(State.NULL);
					}
					
					this.logger.fine("creating new VideoComponent.");
					_runInUiThread(this.videoFrameParent, new Runnable() {
						@Override
						public void run() {
							PlaybackEngine.this.videoComponent = new VideoComponent(PlaybackEngine.this.videoFrameParent, SWT.NO_BACKGROUND);
							PlaybackEngine.this.videoComponent.setKeepAspect(true);
							PlaybackEngine.this.videoComponent.expose(); // This is important on Ubuntu 10.10 (not needed on 10.04).
							PlaybackEngine.this.videoElement = PlaybackEngine.this.videoComponent.getElement();
							PlaybackEngine.this.playbin.setVideoSink(PlaybackEngine.this.videoElement);
							PlaybackEngine.this.videoFrameParent.layout();
							
							PlaybackEngine.this.videoComponent.addKeyListener(PlaybackEngine.this.keyListener);
							PlaybackEngine.this.videoComponent.addMouseListener(PlaybackEngine.this.mouseListener);
						}
					});
					
					/*
					 * If video was playing, put it back again...
					 */
					if (seek && position>=0) {
						this.playbin.setState(State.PAUSED);
						
						if (position > 0) {
							long startTime = System.currentTimeMillis();
							
							while (true) {
								this.playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, position, SeekType.NONE, -1);
								
								if (this.playbin.queryPosition(TimeUnit.NANOSECONDS) > 0) { break; }
								try { Thread.sleep(200); } catch (InterruptedException e) { /* UNUSED */ }
								if (this.playbin.queryPosition(TimeUnit.NANOSECONDS) > 0) { break; }
							}
							
							this.logger.fine("Seek took " + (System.currentTimeMillis() - startTime) + " ms.");
						}
						
						if (state != State.PAUSED) {
							this.playbin.setState(state);
						}
					}
				}
				else { // If there is no video or nowhere to put video...
					this.logger.fine("reparentVideo() : setVideoSink(fakesink).");
					Element fakesink = ElementFactory.make("fakesink", "videosink");
					fakesink.set("sync", new Boolean(true));
					this.playbin.setVideoSink(fakesink); // If we had video and now don't, remove it.
				}
			}
			
			// If we left stuff that needed disposing, do so here.
			this.logger.fine("cleanup...");
			if (old_videoElement != null) {
				old_videoElement.dispose();
			}
			if (old_videoComponent != null && !old_videoComponent.isDisposed()) {
				_runInUiThread(old_videoComponent, new Runnable() {
					@Override
					public void run() {
						Composite parent = old_videoComponent.getParent();
						old_videoComponent.dispose();
						parent.layout();
					}
				});
			}
			
		}
		finally {
			this.playLock.unlock();
		}
		
		this.logger.exiting(this.getClass().getName(), "reparentVideo");
	}
	
	private Bus.EOS eosBus = new Bus.EOS() {
		@Override
		public void endOfStream(GstObject source) {
			PlaybackEngine.this.logger.entering(this.getClass().getName(), "endOfStream");
			
			if (source == PlaybackEngine.this.playbin) {
				handleEosEvent("g");
			}
			
			PlaybackEngine.this.logger.exiting(this.getClass().getName(), "endOfStream");
		}
	};
	
	private Bus.STATE_CHANGED stateChangedBus = new Bus.STATE_CHANGED() {
		@Override
		public void stateChanged(GstObject source, State old, State current, State pending) {
			if (source == PlaybackEngine.this.playbin) {
				switch (current) {
				case NULL:
					callStateListener(PlayState.Stopped);
					break;
					
				case PLAYING:
					callStateListener(PlayState.Playing);
					break;
					
				case PAUSED:
					callStateListener(PlayState.Paused);
					break;
					
				case READY:
					callStateListener(PlayState.Stopped); // FIXME add "Loaded" to enum?
					break;
					
				}
			}
		}
	};
	
	KeyListener keyListener = new KeyListener() {
		@Override
		public void keyReleased(KeyEvent key) {
			callOnKeyPressListener(key.keyCode);
		}
		@Override
		public void keyPressed(KeyEvent arg0) { /* UNUSED */ }
	};
	
	MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			callOnClickListener(e.button, e.count);
		}
		@Override
		public void mouseUp(MouseEvent arg0) { /* UNUSED */ }
		@Override
		public void mouseDown(MouseEvent arg0) { /* UNUSED */ }
	};
	
	
	
	
	private void _startTrack () {
		this.logger.entering(this.getClass().getName(), "_startTrack");
		
		this.playLock.lock();
		try {
			this.m_stopPlaying.set(false);
			this.m_atEos.set(false);
			
			if (this.playbin != null) {
				startWatcherThread();
				
				this.logger.fine("calling playbin.setState(PLAYING)...");
				this.playbin.setState(State.PLAYING);
				callStateListener(PlayState.Playing);
				
				if (this.hasVideo) {
					new WaitForVideoThread().start();
//					reparentVideo(); // I have no idea why this was here or if it was important.
				}
			}
		}
		finally {
			this.playLock.unlock();
		}
		
		this.logger.exiting(this.getClass().getName(), "_startTrack");
	}
	
	private class WaitForVideoThread extends Thread {
		
		public WaitForVideoThread() {
			setDaemon(true);
		}
		
		@Override
		public void run() {
			Element decodeElement = null;
			long startTime = System.currentTimeMillis();
			while (decodeElement == null) {
				if (System.currentTimeMillis() - startTime > WAIT_FOR_decodeElement_TIMEOUT) {
					PlaybackEngine.this.logger.fine("WaitForVideoThread : Timed out waiting for decodeElement to be available.");
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) { /* UNUSED */ }
				
				List<Element> elements = PlaybackEngine.this.playbin.getElements();
				for (Element element : elements) {
					if (element.getName().contains("decodebin")) {
						decodeElement = element;
					}
				}
			}
			
			while (true) {
				boolean check = checkIfVideoFound(decodeElement);
				if (check) {
					PlaybackEngine.this.logger.fine("WaitForVideoThread : Found all pads in " + (System.currentTimeMillis() - startTime) + " ms.");
					break;
				}
				
				if (System.currentTimeMillis() - startTime > 30000) {
					PlaybackEngine.this.logger.fine("WaitForVideoThread : Timed out waiting for checkIfVideoFound to return true.");
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) { /* UNUSED */ }
			}
		}
		
	}
	
	boolean checkIfVideoFound (Element decodeElement) {
		if (!this.hasVideo) {
			this.logger.fine("checkIfVideoFound() : Already concluded no video, aborting checkIfVideoFound.");
			return true;
		}
		
		int srcCount = 0;
		boolean foundVideo = false;
		boolean noMorePads = false;
		
		List<Pad> pads = decodeElement.getPads();
		for (int i = 0; i < pads.size(); i++) {
			Pad pad = pads.get(i);
			this.logger.fine("checkIfVideoFound() : pad["+i+" of "+pads.size()+"]: " + pad.getName());
			
			Caps caps = pad.getCaps();
			if (caps != null) {
				if (caps.size() > 0) {
					Structure structure = caps.getStructure(0);
					if (structure != null) {
						if (structure.getName().startsWith("video/")) {
							foundVideo = true;
						}
					}
				}
			}
			
			if (pad.getName().contains("src")) {
				srcCount++;
			}
			else if (pad.getName().contains("sink") && srcCount > 0) {
				this.logger.fine("checkIfVideoFound() : Found sink pad and at least 1 src pad, assuming noMorePads.");
				noMorePads = true;
				break;
			}
		}
		
		if (noMorePads) {
//			if (srcCount < 2 && hasVideo) {
			if (!foundVideo && this.hasVideo) {
				this.logger.fine("checkIfVideoFound() : Removing video area...");

				this.hasVideo = false;
				this.videoFrameParent.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						reparentVideo();
					}
				});

				this.logger.fine("checkIfVideoFound() : Removed video area.");
			}
			return true;
		}
		
		return false;
	}
	
	
	
	private void pauseTrack () {
		this.playLock.lock();
		try {
			if (this.playbin!=null) {
				this.playbin.setState(State.PAUSED);
				callStateListener(PlayState.Paused);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}
	
	private void resumeTrack () {
		this.playLock.lock();
		try {
			if (this.playbin!=null) {
				this.playbin.setState(State.PLAYING);
				callStateListener(PlayState.Playing);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}
	
	private void stopTrack () {
		this.playLock.lock();
		try {
			stopWatcherThread();
			if (this.playbin!=null) {
				this.playbin.setState(State.NULL);
				callStateListener(PlayState.Stopped);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	private void startWatcherThread () {
		this.logger.entering(this.getClass().getName(), "startWatcherThread");
		
		if (this.watcherThread != null) {
			this.logger.warning("having to stop watcher thread from startWatcherThread().");
			stopWatcherThread();
		}
		
		this.m_stopWatching = false;
		this.watcherThread = new WatcherThread();
		this.watcherThread.setDaemon(true);
		this.watcherThread.start();
		
		this.logger.exiting(this.getClass().getName(), "startWatcherThread");
	}
	
	private void stopWatcherThread () {
		this.logger.entering(this.getClass().getName(), "stopWatcherThread");
		
		if (this.watcherThread != null) {
    		this.m_stopWatching = true;
    		if (this.watcherThread.isAlive()) {
    			this.watcherThread.interrupt();
        		try {
        			if (this.watcherThread!=null && !Thread.currentThread().equals(this.watcherThread)) {
        				this.watcherThread.join();
        			}
        		}
        		catch (InterruptedException e) {
        			e.printStackTrace();
        		}
    		}
    		this.watcherThread = null;
		}
		
		this.logger.exiting(this.getClass().getName(), "stopWatcherThread");
	}
	
	private class WatcherThread extends Thread {
		
		private int lastPositionSec = -1;
		private int lastDurationSec = -1;
		private int eosManCounter = 0;
		
		public WatcherThread () { /* UNUSED */ }
		
		@Override
		public void run() {
			while (!PlaybackEngine.this.m_stopWatching) {
				
				if (PlaybackEngine.this.playbin != null) {
					int positionSec = (int) PlaybackEngine.this.playbin.queryPosition(TimeUnit.SECONDS);
					int durationSec = (int) PlaybackEngine.this.playbin.queryDuration(TimeUnit.SECONDS);
					
					// See if we need to notify owner of progress.
					if (positionSec != this.lastPositionSec) {
						callPositionListener(positionSec);
						this.lastPositionSec = positionSec;
					}
					
					// See if we need to notify owner of duration change.
					if (durationSec != this.lastDurationSec) {
						callDurationListener(durationSec);
						this.lastDurationSec = durationSec;
					}
					
					// End of track and GStreamer has failed to notify us?
					if (durationSec > 0 && positionSec >= (durationSec - EOS_MARGIN_SECONDS)) {
						this.eosManCounter = this.eosManCounter + 1;
						PlaybackEngine.this.logger.fine("eosManCounter++ = " + this.eosManCounter);
						if (this.eosManCounter >= EOS_MAN_LIMIT) {
							this.eosManCounter = 0;
							handleEosEvent("m=" + positionSec + "ns >= " + durationSec + "ns");
						}
					}
					
					// Poke screen saver.
					if (PlaybackEngine.this.videoFrameParent != null
							&& PlaybackEngine.this.hasVideo
							&& PlaybackEngine.this.playbackState == PlayState.Playing
							) {
						ScreenSaver.pokeScreenSaverProtected();
					}
				}
				
				try {
					Thread.sleep(WATCHER_POLL_INTERVAL_MILLIS);
				} catch (InterruptedException e) { /* UNUSED */ }
			}
		}
	}
	
	void handleEosEvent (String debugType) {
		this.logger.entering(this.getClass().getName(), "handleEosEvent", "(type="+debugType+",m_stopPlaying="+this.m_stopPlaying.get()+",m_atEos="+this.m_atEos.get()+")");
		
		stopWatcherThread();
		
		if (!this.m_stopPlaying.get()) {
			if (this.m_atEos.compareAndSet(false, true)) {
				callOnEndOfTrackHandler();
			}
		}
		
		this.logger.exiting(this.getClass().getName(), "handleEosEvent");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
//	private void callOnErrorHandler (Exception e) {
//		if (listener!=null) {
//			listener.onError(e);
//		}
//	}
	
	private void callOnEndOfTrackHandler () {
		this.logger.entering(this.getClass().getName(), "callOnEndOfTrackHandler");
		
		callStateListener(PlayState.Stopped);
		if (this.listener!=null) {
			this.listener.onEndOfTrack();
		}
		
		this.logger.exiting(this.getClass().getName(), "callOnEndOfTrackHandler");
	}
	
	void callStateListener (PlayState state) {
		this.logger.entering(this.getClass().getName(), "callStateListener", state);
		
		this.playbackState = state;
		if (this.listener != null) this.listener.statusChanged(state);
		
		this.logger.exiting(this.getClass().getName(), "callStateListener");
	}
	
	void callPositionListener (long position) {
		if (this.listener!=null) {
			this.listener.positionChanged(position);
		}
	}
	
	@SuppressWarnings("boxing")
	void callDurationListener (int duration) {
		this.logger.entering(this.getClass().getName(), "callDurationListener", duration);
		
		if (this.listener!=null) {
			this.listener.durationChanged(duration);
		}
		
		this.logger.exiting(this.getClass().getName(), "callDurationListener");
	}
	
	void callOnKeyPressListener (int keyCode) {
		if (this.listener!=null) {
			this.listener.onKeyPress(keyCode);
		}
	}
	
	void callOnClickListener (int button, int clickCount) {
		if (this.listener!=null) {
			this.listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI Threading.
	
	private void _runInUiThread (Widget w, Runnable r) {
		if (w.getDisplay().getThread().equals(Thread.currentThread())) {
			r.run();
		}
		else {
			w.getDisplay().syncExec(r);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final static String[] AUDIO_ONLY_FORMATS = {"mp3", "ogg", "wav", "wma", "m4a", "aac", "ra", "mpc", "ac3"};
	
	private boolean mightFileHaveVideo (String f) {
		String ext = f.substring(f.lastIndexOf('.') + 1).toLowerCase();
		for (String e : AUDIO_ONLY_FORMATS) {
			if (e.equals(ext)) {
				this.logger.fine("No video in '"+f+"'.");
				return false;
			}
		}
		this.logger.fine("Might be video in '"+f+"'.");
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

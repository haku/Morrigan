package net.sparktank.morrigan.playbackimpl.gs;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private static final int EOS_MAN_LIMIT = 10; // 10 times.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final AtomicBoolean m_atEos = new AtomicBoolean();
	private final AtomicBoolean m_stopPlaying = new AtomicBoolean();
	
	private String filepath = null;
	Composite videoFrameParent = null;
	private IPlaybackStatusListener listener = null;
	private PlayState playbackState = PlayState.Stopped;
	
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
		if (frame==this.videoFrameParent) return;
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
		if (this.playbin!=null) {
			long duration = this.playbin.queryDuration(TimeUnit.NANOSECONDS);
			this.playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, (long) (d * duration), SeekType.NONE, -1);
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	PlayBin playbin = null;
	VideoComponent videoComponent = null;   // SWT / GStreamer.
	Element videoElement = null;            // GStreamer.
	private volatile boolean hasVideo = false;
	
	private void finalisePlayback () {
		this.logger.fine("finalisePlayback() >>>");
		
		stopWatcherThread();
		
		if (this.playbin!=null) {
			this.playbin.setState(State.NULL);
			this.playbin.dispose();
			this.playbin = null;
			
			if (this.videoElement != null) {
				this.videoElement.dispose();
			}
			
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
		
		this.logger.fine("finalisePlayback() <<<");
	}
	
	private void _loadTrack () throws PlaybackException {
		try {
			callStateListener(PlayState.Loading);
			boolean firstLoad = (this.playbin==null);
			
			this.logger.fine("loadTrack() : firstLoad=" + firstLoad);
			
			if (firstLoad) {
				this.logger.fine("loadTrack() : About to create PlayBin object...");
				this.playbin = new PlayBin("VideoPlayer");
				
				this.logger.fine("loadTrack() : Connecting eosBus...");
				this.playbin.getBus().connect(this.eosBus);
				this.logger.fine("loadTrack() : Connecting stateChangedBus...");
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
			
			this.logger.fine("loadTrack() : About to set input file to '"+this.filepath+"'...");
	        this.playbin.setInputFile(new File(this.filepath));
	        this.logger.fine("loadTrack() : Set file input file.");
	        
        	reparentVideo(false);
		}
		catch (Throwable t) {
			callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+this.filepath+"'.", t);
		}
	}
	
	void reparentVideo () {
		reparentVideo(true);
	}
	
	private void reparentVideo (boolean seek) {
		this.logger.fine("reparentVideo() >>>");
		
		// Clean up any stuff we had before.
		if (this.videoComponent!=null) {
			if (!this.videoComponent.isDisposed()) {
				_runInUiThread(this.videoComponent, new Runnable() {
					@Override
					public void run() {
						PlaybackEngine.this.videoComponent.removeKeyListener(PlaybackEngine.this.keyListener);
						PlaybackEngine.this.videoComponent.removeMouseListener(PlaybackEngine.this.mouseListener);
					}
				});
				this.logger.fine("reparentVideo() : removed listeners.");
			}
		}
		
		// Clear old values but keep references so we can dispose them later.
		final VideoComponent old_videoComponent = this.videoComponent;
		this.videoComponent = null;
		Element old_videoElement = this.videoElement;
		this.videoElement = null;
		
		// Do we have anything to attach video output to?
		if (this.playbin!=null) {
			// Can not attach video to something that is not there...
			if (this.videoFrameParent != null && this.hasVideo) {
				/*
				 * We can not move the video while it is playing, so if it is,
				 * stop it and remember where it was.
				 */
				long position = -1;
				State state = this.playbin.getState();
				if (state==State.PLAYING || state==State.PAUSED) {
					position = this.playbin.queryPosition(TimeUnit.NANOSECONDS);
					this.logger.fine("reparentVideo() : position=" + position);
					this.playbin.setState(State.NULL);
				}
				
				this.logger.fine("reparentVideo() : creating new VideoComponent.");
				_runInUiThread(this.videoFrameParent, new Runnable() {
					@Override
					public void run() {
						PlaybackEngine.this.videoComponent = new VideoComponent(PlaybackEngine.this.videoFrameParent, SWT.NO_BACKGROUND);
						PlaybackEngine.this.videoComponent.setKeepAspect(true);
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
						
						this.logger.fine("reparentVideo() : Seek took " + (System.currentTimeMillis() - startTime) + " ms.");
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
		if (old_videoElement!=null) {
			old_videoElement.dispose();
		}
		if (old_videoComponent!=null && !old_videoComponent.isDisposed()) {
			_runInUiThread(old_videoComponent, new Runnable() {
				@Override
				public void run() {
					Composite parent = old_videoComponent.getParent();
					old_videoComponent.dispose();
					parent.layout();
				}
			});
		}
		
		this.logger.fine("reparentVideo() <<<");
	}
	
	private Bus.EOS eosBus = new Bus.EOS() {
		@Override
		public void endOfStream(GstObject source) {
			PlaybackEngine.this.logger.fine("endOfStream("+source+") >>>");
			
			if (source == PlaybackEngine.this.playbin) {
				handleEosEvent("g");
			}
			
			PlaybackEngine.this.logger.fine("endOfStream() >>>");
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
		this.logger.fine("playTrack() >>>");
		
		this.m_stopPlaying.set(false);
		this.m_atEos.set(false);
		
		if (this.playbin!=null) {
			this.playbin.setState(State.PLAYING);
			this.logger.fine("playTrack() State set to PLAYING.");
			
			callStateListener(PlayState.Playing);
			startWatcherThread();
			
			if (this.hasVideo) {
				new WaitForVideoThread().start();
			}
		}
		
		this.logger.fine("playTrack() <<<");
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
		if (this.playbin!=null) {
			this.playbin.setState(State.PAUSED);
			callStateListener(PlayState.Paused);
		}
	}
	
	private void resumeTrack () {
		if (this.playbin!=null) {
			this.playbin.setState(State.PLAYING);
			callStateListener(PlayState.Playing);
		}
	}
	
	private void stopTrack () {
		stopWatcherThread();
		if (this.playbin!=null) {
			this.playbin.setState(State.NULL);
			callStateListener(PlayState.Stopped);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	long lastPosition = -1;
	int lastDuration = -1;
	int eosManCounter = 0;
	
	private void startWatcherThread () {
		this.logger.fine("startWatcherThread() >>>");
		
		if (this.watcherThread != null) {
			this.logger.warning("WARNING: having to stop watcher thread from startWatcherThread().");
			stopWatcherThread();
		}
		
		this.m_stopWatching = false;
		this.watcherThread = new WatcherThread();
		this.watcherThread.setDaemon(true);
		this.watcherThread.start();
		
		// Make sure that these get reset.
		this.lastPosition = -1;
		this.lastDuration = -1;
		this.eosManCounter = 0;
		
		this.logger.fine("startWatcherThread() <<<");
	}
	
	private void stopWatcherThread () {
		this.logger.fine("stopWatcherThread() >>>");
		
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
		
		this.logger.fine("stopWatcherThread() <<<");
	}
	
	private class WatcherThread extends Thread {
		
		public WatcherThread () { /* UNUSED */ }

		@Override
		public void run() {
			while (!PlaybackEngine.this.m_stopWatching) {
				
				if (PlaybackEngine.this.playbin!=null) {
					long position = PlaybackEngine.this.playbin.queryPosition(TimeUnit.SECONDS);
					
					if (position != PlaybackEngine.this.lastPosition) {
						callPositionListener(position);
						
						if (PlaybackEngine.this.lastPosition > position) {
							PlaybackEngine.this.lastDuration = -1;
						}
						PlaybackEngine.this.lastPosition = position;
						
						if (PlaybackEngine.this.lastDuration < 1) {
							long duration = PlaybackEngine.this.playbin.queryDuration(TimeUnit.SECONDS);
							if (duration > 0) {
								PlaybackEngine.this.lastDuration = (int) duration;
								callDurationListener(PlaybackEngine.this.lastDuration);
							}
						}
					}
					
					if (PlaybackEngine.this.lastDuration > 0 && position >= PlaybackEngine.this.lastDuration) {
						PlaybackEngine.this.eosManCounter++;
						PlaybackEngine.this.logger.fine("eosManCounter++ = " + PlaybackEngine.this.eosManCounter);
						if (PlaybackEngine.this.eosManCounter >= EOS_MAN_LIMIT) {
							PlaybackEngine.this.eosManCounter = 0;
							handleEosEvent("m=" + position + ">=" + PlaybackEngine.this.lastDuration);
						}
					}
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) { /* UNUSED */ }
			}
		}
	}
	
	void handleEosEvent (String debugType) {
		this.logger.fine("handleEosEvent(type="+debugType+",m_stopPlaying="+this.m_stopPlaying.get()+",m_atEos="+this.m_atEos.get()+") >>>");
		
		stopWatcherThread();
		
		if (!this.m_stopPlaying.get()) {
			if (this.m_atEos.compareAndSet(false, true)) {
				callOnEndOfTrackHandler();
			}
		}
		
		this.logger.fine("handleEosEvent() <<<");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
//	private void callOnErrorHandler (Exception e) {
//		if (listener!=null) {
//			listener.onError(e);
//		}
//	}
	
	private void callOnEndOfTrackHandler () {
		this.logger.fine("callOnEndOfTrackHandler() >>>");
		
		callStateListener(PlayState.Stopped);
		if (this.listener!=null) {
			this.listener.onEndOfTrack();
		}
		
		this.logger.fine("callOnEndOfTrackHandler() <<<");
	}
	
	void callStateListener (PlayState state) {
		this.logger.fine("callStateListener("+state.name()+") >>>");
		
		this.playbackState = state;
		if (this.listener!=null) this.listener.statusChanged(state);
		
		this.logger.fine("callStateListener() <<<");
	}
	
	void callPositionListener (long position) {
		if (this.listener!=null) {
			this.listener.positionChanged(position);
		}
	}
	
	void callDurationListener (int duration) {
		this.logger.fine("callDurationListener("+duration+") >>>");
		
		if (this.listener!=null) {
			this.listener.durationChanged(duration);
		}
		
		this.logger.fine("callDurationListener() <<<");
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
				this.logger.fine("mightFileHaveVideo() : No video in '"+f+"'.");
				return false;
			}
		}
		this.logger.fine("mightFileHaveVideo() : Might be video in '"+f+"'.");
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

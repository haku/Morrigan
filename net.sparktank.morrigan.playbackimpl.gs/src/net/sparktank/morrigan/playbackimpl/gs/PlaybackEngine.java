package net.sparktank.morrigan.playbackimpl.gs;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Format;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
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
	
	// TODO Any more?
	private final static String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private volatile boolean m_stopPlaying;
	
	private String filepath = null;
	private Composite videoFrameParent = null;
	private IPlaybackStatusListener listener = null;
	private PlayState playbackState = PlayState.Stopped;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.

	public PlaybackEngine () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.
	
	@Override
	public String getAbout () {
		return "net.sparktank.morrigan.playbackimpl.dsj version 0.01.";
	}
	
	@Override
	public String[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	@Override
	public int readFileDuration(final String filepath) throws PlaybackException {
		initGst();
		
		PlayBin playb = new PlayBin("Metadata");
		playb.setVideoSink(ElementFactory.make("fakesink", "videosink"));
		playb.setInputFile(new File(filepath));
		playb.setState(State.PAUSED);
		
		long queryDuration = -1;
		long startTime = System.currentTimeMillis();
		while (true) {
			queryDuration = playb.queryDuration(TimeUnit.SECONDS);
			if (queryDuration > 0 || System.currentTimeMillis() - startTime > 5000) {
				break;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {}
		}
		playb.setState(State.NULL);
		playb.dispose();
		
		return (int) queryDuration;
	}
	
	@Override
	public void setClassPath(File[] arg0) {}
	
	@Override
	public void setFile(String filepath) {
		this.filepath = filepath;
	}
	
	@Override
	public void setVideoFrameParent (Composite frame) {
		if (frame==videoFrameParent) return;
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
	public void startPlaying() throws PlaybackException {
		System.err.println("gs.startPlaying() called on thread " + Thread.currentThread().getId() + " : " + Thread.currentThread().getName());
		
		m_stopPlaying = false;
		
		try {
			loadTrack();
		} catch (Throwable t) {
			callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+filepath+"'.", t);
		}
		
		playTrack();
	}
	
	@Override
	public void stopPlaying() throws PlaybackException {
		m_stopPlaying = true;
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
		return playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		return (int) playbin.queryDuration(TimeUnit.SECONDS);
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		return playbin.queryPosition(TimeUnit.SECONDS);
	}
	
	public void seekTo(double d) throws PlaybackException {
		if (playbin!=null) {
			long duration = playbin.queryDuration(TimeUnit.NANOSECONDS);
			playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH, SeekType.SET, (long) (d * duration), SeekType.NONE, -1);
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	private PlayBin playbin = null;
	private VideoComponent videoComponent = null;
	private Element videoElement = null;
	private volatile boolean hasVideo = false;
	
	private boolean inited = false;
	
	private void initGst () {
		if (inited) return;
		Gst.init("VideoPlayer", new String[] {});
		inited = true;
	}
	
	private void finalisePlayback () {
		System.err.println("finalisePlayback()");
		
		if (playbin!=null) {
			playbin.setState(State.NULL);
			playbin.dispose();
			playbin = null;
			
			if (videoElement != null) {
				videoElement.dispose();
			}
			
			if (videoComponent!=null) {
				if (!videoComponent.isDisposed()) {
					videoComponent.removeKeyListener(keyListener);
					videoComponent.removeMouseListener(mouseListener);
					videoComponent.dispose();
				}
				videoComponent = null;
				videoFrameParent.redraw();
			}
		}
	}
	
	private void loadTrack () {
		callStateListener(PlayState.Loading);
		boolean firstLoad = (playbin==null);
		
		System.err.println("firstLoad=" + firstLoad);
		
		if (firstLoad) {
			System.err.println("initGst()...");
			initGst();
			System.err.println("About to create PlayBin object...");
			playbin = new PlayBin("VideoPlayer");
			
			System.err.println("Connecting eosBus...");
			playbin.getBus().connect(eosBus);
			System.err.println("Connecting stateChangedBus...");
			playbin.getBus().connect(stateChangedBus);
//			playbin.getBus().connect(durationBus);
			
		} else {
			playbin.setState(State.NULL);
		}
		
		hasVideo = false; // FIXME set this to false.
		
		System.err.println("About to set input file to '"+filepath+"'...");
        playbin.setInputFile(new File(filepath));
        System.err.println("Set file input file.");
        
        reparentVideo();
	}
	
	private void reparentVideo () {
		reparentVideo(true);
	}
	
	private void reparentVideo (boolean seek) {
		System.err.println("Entering reparentVideo()");
		
		if (videoComponent!=null) {
			if (!videoComponent.isDisposed()) {
				videoComponent.removeKeyListener(keyListener);
				videoComponent.removeMouseListener(mouseListener);
			}
		}
		
		VideoComponent old_videoComponent = videoComponent;
		videoComponent = null;
		
		Element old_videoElement = videoElement;
		videoElement = null;
		
		if (playbin!=null && hasVideo) {
			long position = -1;
			State state = playbin.getState();
			if (state==State.PLAYING || state==State.PAUSED) {
				position = playbin.queryPosition(TimeUnit.NANOSECONDS);
				System.err.println("position=" + position);
				playbin.setState(State.NULL);
			}
			
			videoComponent = new VideoComponent(videoFrameParent, SWT.NO_BACKGROUND);
			videoComponent.setKeepAspect(true);
			videoElement = videoComponent.getElement();
			playbin.setVideoSink(videoElement);
			videoFrameParent.layout();
			
			videoComponent.addKeyListener(keyListener);
			videoComponent.addMouseListener(mouseListener);
			
			if (seek && position>=0) {
				playbin.setState(State.PAUSED);
				
				if (position > 0) {
					long startTime = System.currentTimeMillis();
					
					while (true) {
						playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH, SeekType.SET, position, SeekType.NONE, -1);

						if (playbin.queryPosition(TimeUnit.NANOSECONDS) > 0) {
							break;
						}
						
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {}

						if (playbin.queryPosition(TimeUnit.NANOSECONDS) > 0) {
							break;
						}
					}
					
					System.err.println("Seek took " + (System.currentTimeMillis() - startTime) + " ms.");
				}
				
				if (state != State.PAUSED) {
					playbin.setState(state);
				}
			}
			
		}
		
		if (old_videoElement!=null) {
			old_videoElement.dispose();
		}
		
		if (old_videoComponent!=null && !old_videoComponent.isDisposed()) {
			Composite parent = old_videoComponent.getParent();
			old_videoComponent.dispose();
			parent.layout();
		}
		
		System.err.println("leaving reparentVideo()");
	}
	
	private Bus.EOS eosBus = new Bus.EOS() {
		public void endOfStream(GstObject source) {
			if (source == playbin) {
				if (!m_stopPlaying) {
					callOnEndOfTrackHandler();
				}
			}
		}
	};
	
	private Bus.STATE_CHANGED stateChangedBus = new Bus.STATE_CHANGED() {
		public void stateChanged(GstObject source, State old, State current, State pending) {
			if (source == playbin) {
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
	
//	private Bus.DURATION durationBus = new Bus.DURATION() {
//		@Override
//		public void durationChanged(GstObject source, Format format, long duration) {
//			if (source == playbin) {
//				System.err.println("durationBus: duration=" + duration);
//			}
//		}
//	};
	
	private KeyListener keyListener = new KeyListener() {
		@Override
		public void keyReleased(KeyEvent key) {
			callOnKeyPressListener(key.keyCode);
		}
		@Override
		public void keyPressed(KeyEvent arg0) {}
	};
	
	private MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			callOnClickListener(e.button, e.count);
		}
		@Override
		public void mouseUp(MouseEvent arg0) {}
		@Override
		public void mouseDown(MouseEvent arg0) {}
	};
	
	private void playTrack () {
		System.err.println("Entering playTrack().");
		
		if (playbin!=null) {
			
			actuallyStartedPlaying = false;
			
			playbin.setVideoSink(null);
			playbin.setState(State.PAUSED);
	        
			String decodeElementName = null;
			
	        List<Element> elements = playbin.getElements();
			for (Element element : elements) {
				if (element.getName().contains("decodebin")) {
					decodeElementName = element.getName();
				}
			}
			
			if (decodeElementName == null) {
				throw new NullPointerException("decodeElement==null");
			}
			
			final Element decodeElement = playbin.getElementByName(decodeElementName);
			
			decodeElement.connect(new Element.PAD_ADDED() {
				@Override
				public void padAdded(Element source, Pad pad) {
					if (pad.isLinked()) return;
					
					if (pad.getCaps().getStructure(0).getName().startsWith("video/")) {
						System.err.println("Track has video stream.");
						hasVideo = true;
					}
				}
			});
			
			decodeElement.connect(noMOREPADS);
			
			// FIXME this timeout is very ugly.
			videoFrameParent.getDisplay().timerExec(5000, new Runnable() {
				@Override
				public void run() {
					System.err.println("WARNING! using timout to prod playback start because NO_MORE_PADS did not fire.");
					if (!actuallyStartedPlaying) {
						noMOREPADS.noMorePads(decodeElement);
					}
				}
			});
			
		}
		
		System.err.println("Leaving playTrack().");
	}
	
	private Element.NO_MORE_PADS noMOREPADS = new Element.NO_MORE_PADS() {
		@Override
		public void noMorePads(Element element) {
			System.err.println("[debug] no more pads!");
			
			if (hasVideo == false) {
				List<Pad> pads = element.getPads();
				for (Pad pad : pads) {
					System.err.println("pad: " + pad.getName());

					try {
						if (pad.getCaps().getStructure(0).getName().startsWith("video/")) {
							System.err.println("Track has video stream.");
							hasVideo = true;
						}
					} catch (NullPointerException e) {
						System.err.println("NPE.");
					}
				}
			}
			
			if (hasVideo) {
				videoFrameParent.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						reparentVideo(false);
						_actuallyStartPlaying();
					}
				});
				
			} else {
				_actuallyStartPlaying();
			}
			
		}
	};
	
	private Object actuallyStartedPlayingLock = new Object();
	private volatile boolean actuallyStartedPlaying;
	
	private void _actuallyStartPlaying () {
		System.err.println("Entering _actuallyStartPlaying().");
		
		synchronized (actuallyStartedPlayingLock) {
			if (actuallyStartedPlaying) {
				System.err.println("_actuallyStartPlaying() has already been called.");
				return;
			}
			actuallyStartedPlaying = true;
		}
		
		playbin.setState(State.PLAYING);
		callStateListener(PlayState.Playing);
		startWatcherThread();
		
		System.err.println("Leaving _actuallyStartPlaying().");
	}
	
	private void pauseTrack () {
		if (playbin!=null) {
			playbin.setState(State.PAUSED);
			callStateListener(PlayState.Paused);
		}
	}
	
	private void resumeTrack () {
		if (playbin!=null) {
			playbin.setState(State.PLAYING);
			callStateListener(PlayState.Playing);
		}
	}
	
	private void stopTrack () {
		stopWatcherThread();
		if (playbin!=null) {
			playbin.setState(State.NULL);
			callStateListener(PlayState.Stopped);
		}
	}
	
	private volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	private void startWatcherThread () {
		System.err.println("Entering startWatcherThread().");
		
		m_stopWatching = false;
		watcherThread = new WatcherThread();
		watcherThread.setDaemon(true);
		watcherThread.start();
		
		System.err.println("Leaving startWatcherThread().");
	}
	
	private void stopWatcherThread () {
		m_stopWatching = true;
		try {
			if (watcherThread!=null
					&& watcherThread.isAlive()
					&& !Thread.currentThread().equals(watcherThread)) {
				watcherThread.join();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private long lastPosition = -1;
	private int lastDuration = -1;
	
	private class WatcherThread extends Thread {
		public void run() {
			while (!m_stopWatching) {
				
				if (playbin!=null) {
					long position = playbin.queryPosition(TimeUnit.SECONDS);
					if (position != lastPosition) {
						callPositionListener(position);
						
						if (lastPosition > position) {
							lastDuration = -1;
						}
						
						if (lastDuration < 1) {
							long duration = playbin.queryDuration(TimeUnit.SECONDS);
							if (duration > 0) {
								lastDuration = (int) duration;
								callDurationListener(lastDuration);
							}
						}
						
						lastPosition = position;
					}
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
//	private void callOnErrorHandler (Exception e) {
//		if (listener!=null) {
//			listener.onError(e);
//		}
//	}
	
	private void callOnEndOfTrackHandler () {
		callStateListener(PlayState.Stopped);
		if (listener!=null) {
			listener.onEndOfTrack();
		}
	}
	
	private void callStateListener (PlayState state) {
		System.err.println("Entering callStateListener("+state.name()+").");
		
		this.playbackState = state;
		if (listener!=null) listener.statusChanged(state);
		
		System.err.println("Leaving callStateListener().");
	}
	
	private void callPositionListener (long position) {
		if (listener!=null) {
			listener.positionChanged(position);
		}
	}
	
	private void callDurationListener (int duration) {
		System.err.println("Entering callDurationListener("+duration+").");
		
		if (listener!=null) {
			listener.durationChanged(duration);
		}
		
		System.err.println("Entering callDurationListener().");
	}
	
	private void callOnKeyPressListener (int keyCode) {
		if (listener!=null) {
			listener.onKeyPress(keyCode);
		}
	}
	
	private void callOnClickListener (int button, int clickCount) {
		if (listener!=null) {
			listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

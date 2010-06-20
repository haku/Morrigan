package net.sparktank.morrigan.playbackimpl.gs;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Format;
import org.gstreamer.Gst;
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
	
	// TODO Any more?
	private final static String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final AtomicBoolean m_atEos = new AtomicBoolean();
	private final AtomicBoolean m_stopPlaying = new AtomicBoolean();
	
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
	public void loadTrack() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		_loadTrack();
		long l0 = System.currentTimeMillis() - t0;
		System.err.println("Track load time: "+l0+" ms.");
	}
	
	@Override
	public void startPlaying() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		_startTrack();
		long l0 = System.currentTimeMillis() - t0;
		System.err.println("Track start time: "+l0+" ms.");
	}
	
	@Override
	public void stopPlaying() throws PlaybackException {
		m_stopPlaying.set(true);
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
			playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, (long) (d * duration), SeekType.NONE, -1);
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	private PlayBin playbin = null;
	private VideoComponent videoComponent = null;   // SWT / GStreamer.
	private Element videoElement = null;            // GStreamer.
	private volatile boolean hasVideo = false;
	
	private boolean inited = false;
	
	private void initGst () {
		if (inited) return;
		Gst.init("VideoPlayer", new String[] {});
		inited = true;
	}
	
	private void finalisePlayback () {
		System.err.println("finalisePlayback() >>>");
		
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
		
		System.err.println("finalisePlayback() <<<");
	}
	
	private void _loadTrack () throws PlaybackException {
		try {
			callStateListener(PlayState.Loading);
			boolean firstLoad = (playbin==null);
			
			System.err.println("loadTrack() : firstLoad=" + firstLoad);
			
			if (firstLoad) {
				System.err.println("loadTrack() : initGst()...");
				initGst();
				System.err.println("loadTrack() : About to create PlayBin object...");
				playbin = new PlayBin("VideoPlayer");
				
				System.err.println("loadTrack() : Connecting eosBus...");
				playbin.getBus().connect(eosBus);
				System.err.println("loadTrack() : Connecting stateChangedBus...");
				playbin.getBus().connect(stateChangedBus);
				
			} else {
				playbin.setState(State.NULL);
			}
			
			hasVideo = mightFileHaveVideo(filepath);
			
			System.err.println("loadTrack() : About to set input file to '"+filepath+"'...");
	        playbin.setInputFile(new File(filepath));
	        System.err.println("loadTrack() : Set file input file.");
	        
	        reparentVideo(false);
		}
		catch (Throwable t) {
			callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+filepath+"'.", t);
		}
	}
	
	private void reparentVideo () {
		reparentVideo(true);
	}
	
	private void reparentVideo (boolean seek) {
		System.err.println("reparentVideo() >>>");
		
		if (videoComponent!=null) {
			if (!videoComponent.isDisposed()) {
				videoComponent.removeKeyListener(keyListener);
				videoComponent.removeMouseListener(mouseListener);
				System.err.println("reparentVideo() : removed listeners.");
			}
		}
		
		VideoComponent old_videoComponent = videoComponent;
		videoComponent = null;
		
		Element old_videoElement = videoElement;
		videoElement = null;
		
		if (playbin!=null) {
			if (hasVideo) {
				long position = -1;
				State state = playbin.getState();
				if (state==State.PLAYING || state==State.PAUSED) {
					position = playbin.queryPosition(TimeUnit.NANOSECONDS);
					System.err.println("reparentVideo() : position=" + position);
					playbin.setState(State.NULL);
				}

				System.err.println("reparentVideo() : creating new VideoComponent.");
				
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
							playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, position, SeekType.NONE, -1);

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

						System.err.println("reparentVideo() : Seek took " + (System.currentTimeMillis() - startTime) + " ms.");
					}

					if (state != State.PAUSED) {
						playbin.setState(state);
					}
				}

			} else {
				System.err.println("reparentVideo() : setVideoSink(null).");
				playbin.setVideoSink(null); // If we had video and now don't, remove it.
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
		
		System.err.println("reparentVideo() <<<");
	}
	
	private Bus.EOS eosBus = new Bus.EOS() {
		public void endOfStream(GstObject source) {
			System.err.println("endOfStream("+source+") >>>");
			
			if (source == playbin) {
				handleEosEvent();
			}
			
			System.err.println("endOfStream() >>>");
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
	
	
	
	
	private void _startTrack () {
		System.err.println("playTrack() >>>");
		
		m_stopPlaying.set(false);
		m_atEos.set(false);
		
		if (playbin!=null) {
			playbin.setState(State.PLAYING);
			callStateListener(PlayState.Playing);
			startWatcherThread();
			
			if (hasVideo) {
				new WaitForVideoThread().start();
			}
		}
		
		System.err.println("playTrack() <<<");
	}
	
	private class WaitForVideoThread extends Thread {
		
		public WaitForVideoThread() {
			setDaemon(true);
		}
		
		public void run() {
			Element decodeElement = null;
			long startTime = System.currentTimeMillis();
			while (decodeElement == null) {
				if (System.currentTimeMillis() - startTime > 30000) {
					System.err.println("WaitForVideoThread : Timed out waiting for decodeElement to be available.");
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
				
				List<Element> elements = playbin.getElements();
				for (Element element : elements) {
					if (element.getName().contains("decodebin")) {
						decodeElement = element;
					}
				}
			}
			
			while (true) {
				boolean check = checkIfVideoFound(decodeElement);
				if (check) {
					System.err.println("WaitForVideoThread : Found all pads in " + (System.currentTimeMillis() - startTime) + ".");
					break;
				}
				
				if (System.currentTimeMillis() - startTime > 30000) {
					System.err.println("WaitForVideoThread : Timed out waiting for checkIfVideoFound to return true.");
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
		}
		
	}
	
	private boolean checkIfVideoFound (Element decodeElement) {
		if (!hasVideo) {
			System.err.println("checkIfVideoFound() : Already concluded no video, aborting checkIfVideoFound.");
			return true;
		}
		
		int srcCount = 0;
		boolean foundVideo = false;
		boolean noMorePads = false;
		
		List<Pad> pads = decodeElement.getPads();
		for (int i = 0; i < pads.size(); i++) {
			Pad pad = pads.get(i);
			System.err.println("checkIfVideoFound() : pad["+i+" of "+pads.size()+"]: " + pad.getName());
			
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
				
			} else if (pad.getName().contains("sink") && srcCount > 0) {
				System.err.println("checkIfVideoFound() : Found sink pad and at least 1 src pad, assuming noMorePads.");
				noMorePads = true;
				break;
			}
		}
		
		if (noMorePads) {
//			if (srcCount < 2 && hasVideo) {
			if (!foundVideo && hasVideo) {
				System.err.println("checkIfVideoFound() : Removing video area...");

				hasVideo = false;
				videoFrameParent.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						reparentVideo();
					}
				});

				System.err.println("checkIfVideoFound() : Removed video area.");
			}
			return true;
		} else {
			return false;
		}
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
		System.err.println("startWatcherThread() >>>");
		
		m_stopWatching = false;
		watcherThread = new WatcherThread();
		watcherThread.setDaemon(true);
		watcherThread.start();
		
		System.err.println("startWatcherThread() <<<");
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
						lastPosition = position;
						
						if (lastDuration < 1) {
							long duration = playbin.queryDuration(TimeUnit.SECONDS);
							if (duration > 0) {
								lastDuration = (int) duration;
								callDurationListener(lastDuration);
							}
						}
						
						if (lastDuration > 0 && position >= lastDuration) {
							handleEosEvent();
						}
					}
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	private void handleEosEvent () {
		System.err.println("handleEosEvent("+m_stopPlaying.get()+","+m_atEos.get()+") >>>");
		
		if (!m_stopPlaying.get()) {
			if (m_atEos.compareAndSet(false, true)) {
				callOnEndOfTrackHandler();
			}
		}
		
		System.err.println("handleEosEvent() <<<");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
//	private void callOnErrorHandler (Exception e) {
//		if (listener!=null) {
//			listener.onError(e);
//		}
//	}
	
	private void callOnEndOfTrackHandler () {
		System.err.println("callOnEndOfTrackHandler() >>>");
		
		callStateListener(PlayState.Stopped);
		if (listener!=null) {
			listener.onEndOfTrack();
		}
		
		System.err.println("callOnEndOfTrackHandler() <<<");
	}
	
	private void callStateListener (PlayState state) {
		System.err.println("callStateListener("+state.name()+") >>>");
		
		this.playbackState = state;
		if (listener!=null) listener.statusChanged(state);
		
		System.err.println("callStateListener() <<<");
	}
	
	private void callPositionListener (long position) {
		if (listener!=null) {
			listener.positionChanged(position);
		}
	}
	
	private void callDurationListener (int duration) {
		System.err.println("callDurationListener("+duration+") >>>");
		
		if (listener!=null) {
			listener.durationChanged(duration);
		}
		
		System.err.println("callDurationListener() <<<");
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
	
	private final static String[] AUDIO_ONLY_FORMATS = {"mp3", "ogg", "wav", "wma", "m4a", "aac", "ra", "mpc", "ac3"};
	
	private boolean mightFileHaveVideo (String f) {
		String ext = f.substring(f.lastIndexOf('.') + 1).toLowerCase();
		for (String e : AUDIO_ONLY_FORMATS) {
			if (e.equals(ext)) {
				System.err.println("mightFileHaveVideo() : No video in '"+f+"'.");
				return false;
			}
		}
		System.err.println("mightFileHaveVideo() : Might be video in '"+f+"'.");
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

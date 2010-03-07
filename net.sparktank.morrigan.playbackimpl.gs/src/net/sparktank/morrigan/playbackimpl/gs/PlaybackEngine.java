package net.sparktank.morrigan.playbackimpl.gs;

import java.io.File;
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
import org.gstreamer.Format;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
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
	public int readFileDuration(String filepath) throws PlaybackException {
		return -1;
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
	public void finalise() {}

	@Override
	public void startPlaying() throws PlaybackException {
		m_stopPlaying = false;
		
		try {
			loadTrack();
		} catch (Exception e) {
			callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+filepath+"'.", e);
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
	
	private void finalisePlayback () {
		System.out.println("finalisePlayback()");
		
		if (playbin!=null) {
			playbin.setState(State.NULL);
			playbin.dispose();
			playbin = null;
			
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
		
		System.out.println("firstLoad=" + firstLoad);
		
		if (firstLoad) {
			Gst.init("VideoPlayer", new String[] {});
			playbin = new PlayBin("VideoPlayer");
			
			playbin.getBus().connect(eosBus);
			playbin.getBus().connect(stateChangedBus);
			playbin.getBus().connect(durationBus);
			
			reparentVideo();
			
		} else {
			playbin.setState(State.NULL);
			reparentVideo();
		}
		
        playbin.setInputFile(new File(filepath));
	}
	
	private void reparentVideo () {
		System.out.println("reparentVideo()");
		
		if (videoComponent!=null) {
			if (!videoComponent.isDisposed()) {
				videoComponent.removeKeyListener(keyListener);
				videoComponent.removeMouseListener(mouseListener);
			}
		}
		
		VideoComponent old_videoComponent = videoComponent;
		videoComponent = null;
		
		if (playbin!=null) {
			
			// FIXME only do this if video is present.
			
			long position = -1;
			State state = playbin.getState();
			if (state==State.PLAYING || state==State.PAUSED) {
				position = playbin.queryPosition(TimeUnit.NANOSECONDS);
				System.out.println("position=" + position);
				playbin.setState(State.NULL);
			}
			
			videoComponent = new VideoComponent(videoFrameParent, SWT.NO_BACKGROUND);
			videoComponent.setKeepAspect(true);
			playbin.setVideoSink(videoComponent.getElement());
			videoFrameParent.layout();
			
			videoComponent.addKeyListener(keyListener);
			videoComponent.addMouseListener(mouseListener);
			
			if (position>=0) {
				playbin.setState(state);
				System.out.println("seek=" + playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH, SeekType.SET, position, SeekType.NONE, -1) );
			}
			
		}
		
		if (old_videoComponent!=null) {
			old_videoComponent.dispose();
			videoFrameParent.layout();
		}
		
		System.out.println("leaving reparentVideo()");
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
	
	private Bus.DURATION durationBus = new Bus.DURATION() {
		@Override
		public void durationChanged(GstObject source, Format format, long duration) {
			if (source == playbin) {
				System.out.println("durationBus: duration=" + duration);
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
	
	private void playTrack () {
		if (playbin!=null) {
			playbin.setState(State.PLAYING);
			startWatcherThread();
			callStateListener(PlayState.Playing);
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
		m_stopWatching = false;
		watcherThread = new WatcherThread();
		watcherThread.setDaemon(true);
		watcherThread.start();
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
	
	private class WatcherThread extends Thread {
		public void run() {
			while (!m_stopWatching) {
				
				if (playbin!=null) {
					callPositionListener(playbin.queryPosition(TimeUnit.SECONDS));
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
			}
		};
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
		this.playbackState = state;
		if (listener!=null) listener.statusChanged(state);
	}
	
	private long lastPosition = -1;
	
	private void callPositionListener (long position) {
		if (listener!=null) {
			if (position != lastPosition) {
				listener.positionChanged(position);
			}
			lastPosition = position;
		}
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

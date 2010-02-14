package net.sparktank.morrigan.playbackimpl.gs;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;

import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.PlaybackException;

import org.gstreamer.Bus;
import org.gstreamer.Format;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.State;
import org.gstreamer.elements.PlayBin;
import org.gstreamer.swing.VideoComponent;

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
	private Frame videoFrame = null;
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
	public void setFile(String filepath) {
		this.filepath = filepath;
	}
	
	@Override
	public void setVideoFrame(Frame frame) {
		this.videoFrame = frame;
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
		return -1;
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		return -1;
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
		if (playbin!=null) {
			playbin.setState(State.NULL);
			playbin.dispose();
			playbin = null;
			
			if (videoComponent!=null) {
				videoFrame.remove(videoComponent);
				videoComponent = null;
				videoFrame.invalidate();
			}
		}
	}
	
	private void loadTrack () {
		callStateListener(PlayState.Loading);
		
		Gst.init("VideoPlayer", new String[] {});
        playbin = new PlayBin("VideoPlayer");
        playbin.setInputFile(new File(filepath));
		
        playbin.getBus().connect(new Bus.EOS() {
        	public void endOfStream(GstObject source) {
        		if (source == playbin) {
	        		if (!m_stopPlaying) {
						callOnEndOfTrackHandler();
					}
        		}
        	}
        });
        
        playbin.getBus().connect(new Bus.DURATION() {
			@Override
			public void durationChanged(GstObject source, Format format, long duration) {
				if (source == playbin) {
					callPositionListener(duration);
				}
			}
		});
        
        playbin.getBus().connect(new Bus.STATE_CHANGED() {
            public void stateChanged(GstObject source, State old, State current, State pending) {
                if (source == playbin) {
                    System.out.println("Pipeline state changed from " + old + " to " + current);
                }
            }
        });
        
        videoComponent = new VideoComponent();
        playbin.setVideoSink(videoComponent.getElement());
        videoFrame.add(videoComponent, BorderLayout.CENTER);
        videoComponent.doLayout();
	}
	
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
//					callPositionListener(dsFiltergraph.getTime() / 1000);
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

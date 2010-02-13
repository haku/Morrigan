package net.sparktank.morrigan.playbackimpl.dsj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;

import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.NotImplementedException;
import net.sparktank.morrigan.playback.PlaybackException;
import de.humatic.dsj.AVCDevice;
import de.humatic.dsj.DSFiltergraph;
import de.humatic.dsj.DSJUtils;
import de.humatic.dsj.DSMovie;

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

	public PlaybackEngine () {
		shoeHorn();
	}
	
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
		throw new NotImplementedException();
	}
	
	@Override
	public PlayState getPlaybackState() {
		return playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		if (dsFiltergraph!=null) {
			return dsFiltergraph.getDuration() / 1000;
		} else {
			return -1;
		}
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		if (dsFiltergraph!=null) {
			return dsFiltergraph.getTime() / 1000;
		} else {
			return -1;
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	private DSFiltergraph dsFiltergraph = null;
	Component videoComponent = null;
	
	private void finalisePlayback () {
		if (dsFiltergraph!=null) {
			dsFiltergraph.dispose();
			dsFiltergraph = null;
			
			if (videoComponent!=null) {
				videoFrame.remove(videoComponent);
				videoComponent.invalidate();
			}
		}
	}
	
	private void loadTrack () {
		dsFiltergraph = new DSMovie(filepath, DSFiltergraph.RENDER_NATIVE, propertyChangeLlistener);
		dsFiltergraph.setVolume(1.0f);
		
		videoComponent = dsFiltergraph.asComponent();
		videoFrame.add(videoComponent, BorderLayout.CENTER);
		videoFrame.doLayout();
	}
	
	private PropertyChangeListener propertyChangeLlistener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent pce) {
			switch (DSJUtils.getEventType(pce)) {
				
				case (AVCDevice.ED_MODE_PLAY):
					callStateListener(PlayState.Playing);
					break;
				
				case (AVCDevice.ED_MODE_STOP):
					callStateListener(PlayState.Stopped);
					break;
				
				case (DSFiltergraph.DONE):
					if (!m_stopPlaying) {
						callOnEndOfTrackHandler();
					}
					break;
				
//				case (DSFiltergraph.GRAPH_ERROR):
//					String message = "Graph error: " + String.valueOf(pce.getNewValue());
//					callOnErrorHandler(new PlaybackException(message));
//					break;
				
			}
		}
	};
	
	private void playTrack () {
		if (dsFiltergraph!=null) {
			dsFiltergraph.play();
			startWatcherThread();
		}
	}
	
	private void stopTrack () {
		stopWatcherThread();
		if (dsFiltergraph!=null) {
			dsFiltergraph.stop();
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
				
				if (dsFiltergraph!=null) {
					callPositionListener(dsFiltergraph.getTime() / 1000);
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
	
	// FIXME this is all REALLY nasty.
	@SuppressWarnings("unchecked")
	private void shoeHorn () {
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible)
			field.setAccessible(true);
			@SuppressWarnings("unused")
			Object original = field.get(clazz);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.setProperty("java.library.path", "D:\\haku\\development\\eclipseWorkspace-java\\dsjtest\\lib");
		System.load("D:\\haku\\development\\eclipseWorkspace-java\\dsjtest\\lib\\dsj.dll");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

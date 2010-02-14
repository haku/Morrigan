package net.sparktank.morrigan.playbackimpl.dsj;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.PlaybackException;
import de.humatic.dsj.DSFiltergraph;
import de.humatic.dsj.DSJUtils;
import de.humatic.dsj.DSMediaType;
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
	private Composite videoFrameParent = null;
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
	public void setVideoFrameParent(Composite frame) {
		this.videoFrameParent = frame;
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
	Composite videoComposite = null;
	Frame videoFrame = null;
	Component videoComponent = null;
	
	private void finalisePlayback () {
		if (dsFiltergraph!=null) {
			dsFiltergraph.dispose();
			dsFiltergraph = null;
			
			if (videoComponent!=null) {
				videoFrame.remove(videoComponent);
				videoFrame.dispose();
				videoFrame = null;
				
				videoComponent.invalidate();
				videoComponent = null;
				
				videoComposite.dispose();
				videoComposite = null;
			}
		}
	}
	
	private void loadTrack () {
		boolean firstLoad = (dsFiltergraph==null);
		
		callStateListener(PlayState.Loading);
		
		if (!firstLoad) {
			finalisePlayback();
			dsFiltergraph = null;
		}
		
		dsFiltergraph = new DSMovie(filepath,
				DSFiltergraph.RENDER_NATIVE, // | DSFiltergraph.INIT_PAUSED,
				propertyChangeLlistener);
		
		dsFiltergraph.setVolume(1.0f);
		
		if (firstLoad && dsFiltergraph.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) {
			videoComponent = dsFiltergraph.asComponent();
			
			videoComposite = new Composite(videoFrameParent, SWT.EMBEDDED);
			videoComposite.setLayout(new FillLayout( ));
	        
			videoFrame = SWT_AWT.new_Frame(videoComposite);
			videoFrame.setBackground(Color.BLACK);
			
			videoFrame.add(videoComponent, BorderLayout.CENTER);
			videoFrame.doLayout();
			
			videoFrameParent.layout();
		}
	}
	
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
			callStateListener(PlayState.Playing);
		}
	}
	
	private void pauseTrack () {
		if (dsFiltergraph!=null) {
			dsFiltergraph.pause();
			callStateListener(PlayState.Paused);
		}
	}
	
	private void resumeTrack () {
		if (dsFiltergraph!=null) {
			dsFiltergraph.play();
			callStateListener(PlayState.Playing);
		}
	}
	
	private void stopTrack () {
		stopWatcherThread();
		if (dsFiltergraph!=null) {
			dsFiltergraph.stop();
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
	
	// FIXME this is all REALLY nasty.
	@SuppressWarnings("unchecked")
	private void shoeHorn () {
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible) field.setAccessible(true);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.setProperty("java.library.path", "D:\\haku\\development\\eclipseWorkspace-java\\dsjtest\\lib");
		
		/* FIXME
		 * This next line fails with
		 * java.lang.UnsatisfiedLinkError: Native Library D:\haku\development\eclipseWorkspace-java\dsjtest\lib\dsj.dll already loaded in another classloader
		 * if it is already loaded.
		 */
		System.load("D:\\haku\\development\\eclipseWorkspace-java\\dsjtest\\lib\\dsj.dll");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.playbackimpl.dsj;

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
import java.lang.reflect.Field;

import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.PlaybackException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

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

	private File[] classPath = null;
	
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
	public void setClassPath(File[] classPath) {
		this.classPath = classPath;
	}
	
	@Override
	public void setFile(String filepath) {
		this.filepath = filepath;
	}
	
	@Override
	public void setVideoFrameParent(Composite frame) {
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
			stopTrack();
			
			if (videoComponent!=null) {
				videoFrame.remove(videoComponent);
				videoFrame.dispose();
				videoFrame = null;
				
				videoComponent.invalidate();
				videoComponent = null;
				
				videoComposite.dispose();
				videoComposite = null;
			}
			
			dsFiltergraph.dispose();
			dsFiltergraph = null;
		}
	}
	
	private void loadTrack () {
		callStateListener(PlayState.Loading);
		boolean firstLoad = (dsFiltergraph==null);
		
		System.out.println("firstLoad=" + firstLoad);
		
		if (firstLoad) {
			shoeHorn();
			
		} else {
			finalisePlayback();
		}
		
		dsFiltergraph = new DSMovie(filepath,
				DSFiltergraph.OVERLAY | DSFiltergraph.MOUSE_ENABLED, // | DSFiltergraph.INIT_PAUSED,
				propertyChangeLlistener);
		dsFiltergraph.setVolume(1.0f);
		
		reparentVideo();
	}
	
	private void reparentVideo () {
		System.out.println("reparentVideo()");
		
//		if (videoComponent!=null) {
//			System.out.println("remove listeners");
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
		
		if (videoFrameParent==null) return;
		if (dsFiltergraph==null) return;
		if (!dsFiltergraph.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) return;
		
		if (videoComponent==null) {
			videoComponent = dsFiltergraph.asComponent();
			videoComponent.setBackground(Color.ORANGE);
			
			System.out.println("adding listeners to videoComponent...");
			videoComponent.addMouseListener(mouseListener);
			videoComponent.addKeyListener(keyListener);
		}
		
		
		if (videoComposite == null || videoComposite.isDisposed()) {
			System.out.println("Making videoComposite...");
			
			videoComposite = new Composite(videoFrameParent, SWT.EMBEDDED);
			videoComposite.setLayout(new FillLayout());
	        
			videoFrame = SWT_AWT.new_Frame(videoComposite);
			videoFrame.setBackground(Color.BLACK);
			
			videoFrame.add(videoComponent, BorderLayout.CENTER);
			videoFrame.doLayout();
			
		} else {
			System.out.println("Moveing videoComposite...");
			videoComposite.setParent(videoFrameParent);
		}
		
		videoFrameParent.layout();
		
		System.out.println("leaving reparentVideo()");
	}
	
	private MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			callOnClickListener(e.getButton(), e.getClickCount());
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
	};
	
	private KeyListener keyListener = new KeyListener() {
		@Override
		public void keyReleased(KeyEvent key) {
			callOnKeyPressListener(key.getKeyCode());
		}
		@Override
		public void keyTyped(KeyEvent arg0) {}
		@Override
		public void keyPressed(KeyEvent arg0) {}
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
	
	private static final String dsjDll = "dsj.dll";
	
	private boolean haveShoeHorned = false;
	
	// FIXME this is all REALLY nasty.
	@SuppressWarnings("unchecked")
	private void shoeHorn () {
		if (haveShoeHorned) return;
		
		File dsjDllFile = null;
		
		for (File classPathFile : classPath) {
			if (classPathFile.isDirectory()) {
				File[] listFiles = classPathFile.listFiles();
				if (listFiles!=null && listFiles.length>0) {
					for (File file : listFiles) {
						if (file.isFile()) {
							if (file.getName().equals(dsjDll)) {
								dsjDllFile = file;
								break;
							}
						}
					}
				}
			}
		}
		
		if (dsjDllFile==null) return;
		System.out.println(dsjDll + "=" + dsjDllFile.getAbsolutePath());
		
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible) field.setAccessible(true);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.setProperty("java.library.path", dsjDllFile.getParentFile().getAbsolutePath());
		
		/* FIXME
		 * This next line fails with
		 * java.lang.UnsatisfiedLinkError: Native Library D:\haku\development\eclipseWorkspace-java\dsjtest\lib\dsj.dll already loaded in another classloader
		 * if it is already loaded.
		 */
		try {
			System.load(dsjDllFile.getAbsolutePath());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		haveShoeHorned = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

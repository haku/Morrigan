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

import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;

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
	public int readFileDuration(String filepath) throws PlaybackException {
		_shoeHorn();
		
		int[] stats = DSJUtils.getBasicFileStats(filepath);
		return stats[0] / 1000;
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
		_reparentVideo();
	}
	
	@Override
	public void unloadFile() {
		_finalisePlayback();
	}
	
	@Override
	public void finalise() {}

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
		return playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		if (dsMovie!=null) {
			return dsMovie.getDuration() / 1000;
		} else {
			return -1;
		}
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		if (dsMovie!=null) {
			return dsMovie.getTime() / 1000;
		} else {
			return -1;
		}
	}
	
	@Override
	public void seekTo(double d) throws PlaybackException {
		if (dsMovie!=null) {
			int duration = dsMovie.getDuration();
			if (duration > 0) {
				dsMovie.setTimeValue((int) (d * duration));
			}
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	private DSMovie dsMovie = null;
	private Composite videoComposite = null;   // SWT.
	private Frame videoFrame = null;           // AWT.
	private Component videoComponent = null;   // AWT.
	
	private void _finalisePlayback () {
		if (dsMovie!=null) {
			_stopTrack();
			
			if (videoComponent!=null) {
				videoFrame.remove(videoComponent);
				videoFrame.dispose();
				videoFrame = null;
				
				videoComponent.invalidate();
				videoComponent = null;
				
				if (!videoComposite.isDisposed()) {
					if (videoComposite.getDisplay().getThread().equals(Thread.currentThread())) {
						videoComposite.dispose();
					} else {
						videoComposite.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								videoComposite.dispose();
							}
						});
					}
				}
				videoComposite = null;
			}
			
			dsMovie.dispose();
			dsMovie = null;
		}
		
		_setScreenSaverActive(true);
	}
	
	private void _loadTrack () throws PlaybackException {
		try {
			_callStateListener(PlayState.Loading);
			boolean firstLoad = (dsMovie==null);
			
			System.err.println("dsj.PlaybackEngine firstLoad=" + firstLoad);
			
			if (firstLoad) {
				_shoeHorn();
			} else {
				_finalisePlayback();
			}
			
			_setScreenSaverActive(false); // FIXME THIS IS A WORKAROUND!
			
			dsMovie = new DSMovie(filepath,
					DSFiltergraph.OVERLAY | DSFiltergraph.MOUSE_ENABLED | DSFiltergraph.INIT_PAUSED,
					propertyChangeLlistener);
			dsMovie.setVolume(1.0f);
			dsMovie.setRecueOnStop(false);
			
			_reparentVideo();
		}
		catch (Exception e) {
			_callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+filepath+"'.", e);
		}
	}
	
	private void _startTrack () {
		System.err.println("dsj.PlaybackEngine playTrack()");
		
		m_stopPlaying = false;
		
		if (dsMovie!=null) {
			/* I wish I knew why this made a difference...
			 * Without doing it like this, it sometimes play video
			 * at high speed and with no sound.
			 * This makes no sense to me.
			 */
			if (videoFrameParent != null) {
				videoFrameParent.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						dsMovie.pause(); // If this is not here, then sometimes playback does not start when play() is called.
						dsMovie.play();
					}
				});
			}
			else {
				dsMovie.play();
			}
			
			_startWatcherThread();
			_callStateListener(PlayState.Playing);
		}
	}
	
	private void _pauseTrack () {
		if (dsMovie!=null) {
			dsMovie.pause();
			_callStateListener(PlayState.Paused);
			_setScreenSaverActive(true);
		}
	}
	
	private void _resumeTrack () {
		if (dsMovie!=null) {
			_setScreenSaverActive(false);
			dsMovie.play();
			_callStateListener(PlayState.Playing);
		}
	}
	
	private void _stopTrack () {
		m_stopPlaying = true;
		_stopWatcherThread();
		if (dsMovie!=null) {
			dsMovie.stop();
			_callStateListener(PlayState.Stopped);
			_setScreenSaverActive(true);
		}
	}
	
	private void _reparentVideo () {
		System.err.println("dsj.PlaybackEngine reparentVideo()");
		
//		if (videoComponent!=null) {
//			System.err.println("remove listeners");
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
		if (dsMovie==null) return;
		if (!dsMovie.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) return;
		
		videoFrameParent.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (videoComponent==null) {
						videoComponent = dsMovie.asComponent();
						videoComponent.setBackground(Color.BLACK);
						
						System.err.println("dsj.PlaybackEngine Adding listeners to videoComponent...");
						videoComponent.addMouseListener(mouseListener);
						videoComponent.addKeyListener(keyListener);
					}
					
					if (videoComposite == null || videoComposite.isDisposed()) {
						System.err.println("dsj.PlaybackEngine Making videoComposite...");
						
						videoComposite = new Composite(videoFrameParent, SWT.EMBEDDED);
						videoComposite.setLayout(new FillLayout());
						
						videoFrame = SWT_AWT.new_Frame(videoComposite);
						videoFrame.setBackground(Color.BLACK);
						
						videoFrame.add(videoComponent, BorderLayout.CENTER);
						videoFrame.doLayout();
						
					} else {
						System.err.println("dsj.PlaybackEngine Moveing videoComposite...");
						videoComposite.setParent(videoFrameParent);
					}
					
					videoFrameParent.layout();
				}
				catch (Exception e) {
					callOnErrorHandler(e);
				}
			}
		});
		
		System.err.println("dsj.PlaybackEngine Leaving reparentVideo()");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Listeners.
	
	private MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			_callOnClickListener(e.getButton(), e.getClickCount());
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
		public void keyTyped(KeyEvent key) {
			_callOnKeyPressListener(key.getKeyCode());
		}
		@Override
		public void keyReleased(KeyEvent key) {}
		@Override
		public void keyPressed(KeyEvent key) {}
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
						_callOnEndOfTrackHandler();
					} else {
						System.err.println("dsj.PlaybackEngine Not calling onEndOfTrackHandler because we are stopping.");
					}
					break;
				
//				case (DSFiltergraph.BUFFERING):
//					System.err.println("dsj.PlaybackEngine DSFiltergraph.BUFFERING"); 
//					break;
				
//				case (DSFiltergraph.BUFFER_COMPLETE):
//					System.err.println("dsj.PlaybackEngine DSFiltergraph.BUFFER_COMPLETE");
//					break;
					
//				case (DSFiltergraph.INITIALIZED):
//					System.err.println("dsj.PlaybackEngine DSFiltergraph.INITIALIZED");
//					break;
					
//				case (DSFiltergraph.IP_READY):
//					System.err.println("dsj.PlaybackEngine DSFiltergraph.IP_READY");
//					break;
					
//				case (DSFiltergraph.OVERLAY_BUFFER_REQUEST):
//					System.err.println("dsj.PlaybackEngine DSFiltergraph.OVERLAY_BUFFER_REQUEST");
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
	
	private volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	private void _startWatcherThread () {
		m_stopWatching = false;
		watcherThread = new WatcherThread();
		watcherThread.setDaemon(true);
		watcherThread.start();
	}
	
	private void _stopWatcherThread () {
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
		
		private int lastMeasuredPosition = -1;
		private int lastSentPosition = -1;
		
		public void run() {
			while (!m_stopWatching) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				
				if (dsMovie!=null) {
					int measuredPosition = dsMovie.getTime();
					if (getPlaybackState() == PlayState.Playing && measuredPosition == lastMeasuredPosition) {
						System.err.println("dsj.PlaybackEngine Prompting playback...");
						dsMovie.play();
					}
					lastMeasuredPosition = measuredPosition;
					
					int position = measuredPosition / 1000;
					if (position != lastSentPosition) {
						_callPositionListener(position);
						lastSentPosition = position;
					}
				}
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	private void callOnErrorHandler (Exception e) {
		if (listener!=null) {
			listener.onError(e);
		}
	}
	
	private void _callOnEndOfTrackHandler () {
		_callStateListener(PlayState.Stopped);
		if (listener!=null) {
			listener.onEndOfTrack();
		}
	}
	
	private void _callStateListener (PlayState state) {
		this.playbackState = state;
		if (listener!=null) listener.statusChanged(state);
	}
	
	private void _callPositionListener (long position) {
		if (listener!=null) {
			listener.positionChanged(position);
		}
	}
	
	private void _callOnKeyPressListener (int keyCode) {
		if (listener!=null) {
			listener.onKeyPress(keyCode);
		}
	}
	
	private void _callOnClickListener (int button, int clickCount) {
		if (listener!=null) {
			listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Screen saver.
	
	private void _setScreenSaverActive (boolean active) {
		if (DSJUtils.getScreenSaverActive()!=active) {
			if (active || (dsMovie!=null && dsMovie.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) ) {
				
				DSJUtils.setScreenSaverActive(active); // FIXME crashes JVM ???
				
				boolean a = DSJUtils.getScreenSaverActive();
				if (active == a) {
					System.err.println("dsj.PlaybackEngine Set screenSaverActive=" + active + ".");
				} else {
					System.err.println("dsj.PlaybackEngine Failed to set screenSaverActive=" + active + ".");
				}
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Runtime DLL loading.
	
	private static final String dsjDll = "dsj.dll";
	
	private boolean haveShoeHorned = false;
	
	// FIXME this is all REALLY nasty.
	@SuppressWarnings("unchecked")
	private void _shoeHorn () {
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
		
		if (dsjDllFile==null) {
			System.err.println("dsj.PlaybackEngine Did not find '" + dsjDll + "'.");
			return;
		}
		System.err.println("dsj.PlaybackEngine dll " + dsjDll + "=" + dsjDllFile.getAbsolutePath());
		
		try {
			Class clazz = ClassLoader.class;
			Field field = clazz.getDeclaredField("sys_paths");
			boolean accessible = field.isAccessible();
			if (!accessible) field.setAccessible(true);
			field.set(clazz, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String newLibPath = System.getProperty("java.library.path") + File.pathSeparator + dsjDllFile.getParentFile().getAbsolutePath();
		System.err.println("Setting java.library.path=" + newLibPath);
		System.setProperty("java.library.path", newLibPath);
		
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
		
		System.err.println("dsj.PlaybackEngine Loaded dll=" + dsjDllFile.getAbsolutePath());
		
		haveShoeHorned = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

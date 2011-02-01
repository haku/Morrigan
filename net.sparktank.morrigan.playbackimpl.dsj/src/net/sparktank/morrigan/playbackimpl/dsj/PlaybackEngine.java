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
	
	protected volatile boolean m_stopPlaying;
	
	private String filepath = null;
	protected Composite videoFrameParent = null;
	private IPlaybackStatusListener listener = null;
	private PlayState playbackState = PlayState.Stopped;

	private File[] classPath = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public PlaybackEngine () {/* UNUSED */}
	
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
	public int readFileDuration(String file) throws PlaybackException {
		_shoeHorn();
		
		int[] stats = DSJUtils.getBasicFileStats(file);
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
		if (frame==this.videoFrameParent) return;
		this.videoFrameParent = frame;
		_reparentVideo();
	}
	
	@Override
	public void unloadFile() {
		_finalisePlayback();
	}
	
	@Override
	public void finalise() {/* UNUSED */}

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
		return this.playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		if (this.dsMovie!=null) {
			return this.dsMovie.getDuration() / 1000;
		}
		return -1;
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		if (this.dsMovie!=null) {
			return this.dsMovie.getTime() / 1000;
		}
		return -1;
	}
	
	@Override
	public void seekTo(double d) throws PlaybackException {
		if (this.dsMovie!=null) {
			int duration = this.dsMovie.getDuration();
			if (duration > 0) {
				this.dsMovie.setTimeValue((int) (d * duration));
			}
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	protected DSMovie dsMovie = null;
	protected Composite videoComposite = null;   // SWT.
	protected Frame videoFrame = null;           // AWT.
	protected Component videoComponent = null;   // AWT.
	
	private void _finalisePlayback () {
		if (this.dsMovie!=null) {
			_stopTrack();
			
			if (this.videoComponent!=null) {
				this.videoFrame.remove(this.videoComponent);
				this.videoFrame.dispose();
				this.videoFrame = null;
				
				this.videoComponent.invalidate();
				this.videoComponent = null;
				
				if (!this.videoComposite.isDisposed()) {
					if (this.videoComposite.getDisplay().getThread().equals(Thread.currentThread())) {
						this.videoComposite.dispose();
					} else {
						this.videoComposite.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								PlaybackEngine.this.videoComposite.dispose();
							}
						});
					}
				}
				this.videoComposite = null;
			}
			
			this.dsMovie.dispose();
			this.dsMovie = null;
		}
		
		_setScreenSaverActive(true);
	}
	
	private void _loadTrack () throws PlaybackException {
		try {
			_callStateListener(PlayState.Loading);
			boolean firstLoad = (this.dsMovie==null);
			
			System.err.println("dsj.PlaybackEngine firstLoad=" + firstLoad);
			
			if (firstLoad) {
				_shoeHorn();
			} else {
				_finalisePlayback();
			}
			
			_setScreenSaverActive(false); // FIXME THIS IS A WORKAROUND!
			
			this.dsMovie = new DSMovie(this.filepath,
					DSFiltergraph.OVERLAY | DSFiltergraph.MOUSE_ENABLED | DSFiltergraph.INIT_PAUSED,
					this.propertyChangeLlistener);
			this.dsMovie.setVolume(1.0f);
			this.dsMovie.setRecueOnStop(false);
			
			_reparentVideo();
		}
		catch (Exception e) {
			_callStateListener(PlayState.Stopped);
			throw new PlaybackException("Failed to load '"+this.filepath+"'.", e);
		}
	}
	
	private void _startTrack () {
		System.err.println("dsj.PlaybackEngine playTrack()");
		
		this.m_stopPlaying = false;
		
		if (this.dsMovie!=null) {
			/* I wish I knew why this made a difference...
			 * Without doing it like this, it sometimes play video
			 * at high speed and with no sound.
			 * This makes no sense to me.
			 */
			if (this.videoFrameParent != null) {
				this.videoFrameParent.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						PlaybackEngine.this.dsMovie.pause(); // If this is not here, then sometimes playback does not start when play() is called.
						PlaybackEngine.this.dsMovie.play();
					}
				});
			}
			else {
				this.dsMovie.play();
			}
			
			_startWatcherThread();
			_callStateListener(PlayState.Playing);
		}
	}
	
	private void _pauseTrack () {
		if (this.dsMovie!=null) {
			this.dsMovie.pause();
			_callStateListener(PlayState.Paused);
			_setScreenSaverActive(true);
		}
	}
	
	private void _resumeTrack () {
		if (this.dsMovie!=null) {
			_setScreenSaverActive(false);
			this.dsMovie.play();
			_callStateListener(PlayState.Playing);
		}
	}
	
	private void _stopTrack () {
		this.m_stopPlaying = true;
		_stopWatcherThread();
		if (this.dsMovie!=null) {
			this.dsMovie.stop();
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
		
		if (this.videoFrameParent==null) return;
		if (this.dsMovie==null) return;
		if (!this.dsMovie.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) return;
		
		this.videoFrameParent.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (PlaybackEngine.this.videoComponent==null) {
						PlaybackEngine.this.videoComponent = PlaybackEngine.this.dsMovie.asComponent();
						PlaybackEngine.this.videoComponent.setBackground(Color.BLACK);
						
						System.err.println("dsj.PlaybackEngine Adding listeners to videoComponent...");
						PlaybackEngine.this.videoComponent.addMouseListener(PlaybackEngine.this.mouseListener);
						PlaybackEngine.this.videoComponent.addKeyListener(PlaybackEngine.this.keyListener);
					}
					
					if (PlaybackEngine.this.videoComposite == null || PlaybackEngine.this.videoComposite.isDisposed()) {
						System.err.println("dsj.PlaybackEngine Making videoComposite...");
						
						PlaybackEngine.this.videoComposite = new Composite(PlaybackEngine.this.videoFrameParent, SWT.EMBEDDED);
						PlaybackEngine.this.videoComposite.setLayout(new FillLayout());
						
						PlaybackEngine.this.videoFrame = SWT_AWT.new_Frame(PlaybackEngine.this.videoComposite);
						PlaybackEngine.this.videoFrame.setBackground(Color.BLACK);
						
						PlaybackEngine.this.videoFrame.add(PlaybackEngine.this.videoComponent, BorderLayout.CENTER);
						PlaybackEngine.this.videoFrame.doLayout();
						
					} else {
						System.err.println("dsj.PlaybackEngine Moveing videoComposite...");
						PlaybackEngine.this.videoComposite.setParent(PlaybackEngine.this.videoFrameParent);
					}
					
					PlaybackEngine.this.videoFrameParent.layout();
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
	
	protected MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			_callOnClickListener(e.getButton(), e.getClickCount());
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {/* UNUSED */}
		@Override
		public void mousePressed(MouseEvent e) {/* UNUSED */}
		@Override
		public void mouseExited(MouseEvent e) {/* UNUSED */}
		@Override
		public void mouseEntered(MouseEvent e) {/* UNUSED */}
	};
	
	protected KeyListener keyListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent key) {
			_callOnKeyPressListener(key.getKeyCode());
		}
		@Override
		public void keyReleased(KeyEvent key) {/* UNUSED */}
		@Override
		public void keyPressed(KeyEvent key) {/* UNUSED */}
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
					if (!PlaybackEngine.this.m_stopPlaying) {
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
	
	protected volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	private void _startWatcherThread () {
		this.m_stopWatching = false;
		this.watcherThread = new WatcherThread();
		this.watcherThread.setDaemon(true);
		this.watcherThread.start();
	}
	
	private void _stopWatcherThread () {
		this.m_stopWatching = true;
		try {
			if (this.watcherThread!=null
					&& this.watcherThread.isAlive()
					&& !Thread.currentThread().equals(this.watcherThread)) {
				this.watcherThread.join();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class WatcherThread extends Thread {
		
		private int lastMeasuredPosition = -1;
		private int lastSentPosition = -1;
		
		public WatcherThread() {/* UNUSED */}
		
		@Override
		public void run() {
			while (!PlaybackEngine.this.m_stopWatching) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {/* UNUSED */}
				
				if (PlaybackEngine.this.dsMovie!=null) {
					int measuredPosition = PlaybackEngine.this.dsMovie.getTime();
					if (getPlaybackState() == PlayState.Playing && measuredPosition == this.lastMeasuredPosition) {
						System.err.println("dsj.PlaybackEngine Prompting playback...");
						PlaybackEngine.this.dsMovie.play();
					}
					this.lastMeasuredPosition = measuredPosition;
					
					int position = measuredPosition / 1000;
					if (position != this.lastSentPosition) {
						_callPositionListener(position);
						this.lastSentPosition = position;
					}
				}
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	protected void callOnErrorHandler (Exception e) {
		if (this.listener!=null) {
			this.listener.onError(e);
		}
	}
	
	protected  void _callOnEndOfTrackHandler () {
		_callStateListener(PlayState.Stopped);
		if (this.listener!=null) {
			this.listener.onEndOfTrack();
		}
	}
	
	private void _callStateListener (PlayState state) {
		this.playbackState = state;
		if (this.listener!=null) this.listener.statusChanged(state);
	}
	
	protected void _callPositionListener (long position) {
		if (this.listener!=null) {
			this.listener.positionChanged(position);
		}
	}
	
	protected void _callOnKeyPressListener (int keyCode) {
		if (this.listener!=null) {
			this.listener.onKeyPress(keyCode);
		}
	}
	
	protected void _callOnClickListener (int button, int clickCount) {
		if (this.listener!=null) {
			this.listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Screen saver.
	
	private void _setScreenSaverActive (boolean active) {
		if (DSJUtils.getScreenSaverActive()!=active) {
			if (active || (this.dsMovie!=null && this.dsMovie.hasMediaOfType(DSMediaType.WMMEDIATYPE_Video)) ) {
				
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
	private void _shoeHorn () {
		if (this.haveShoeHorned) return;
		
		File dsjDllFile = null;
		
		for (File classPathFile : this.classPath) {
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
			Class<?> clazz = ClassLoader.class;
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
		
		this.haveShoeHorned = true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

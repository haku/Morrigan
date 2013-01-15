package com.vaguehope.morrigan.playbackimpl.jmf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.media.bean.playerbean.MediaPlayer;
import javax.media.format.FormatChangeEvent;

import jmapps.ui.VideoPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackException;

public class PlaybackEngine  implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO Any more?
	private static final String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	volatile boolean m_stopPlaying;
	
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
		return "com.vaguehope.morrigan.playbackimpl.jmf version 0.01.";
	}
	
	@Override
	public String[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	@Override
	public int readFileDuration(String fpath) throws PlaybackException {
		this.mediaFile = new File(fpath);
		try {
			Player player = Manager.createRealizedPlayer(this.mediaFile.toURI().toURL());
			Time duration = player.getDuration();
			double seconds = duration.getSeconds();
			player.close();
			
			return (int) seconds;
			
		} catch (Throwable t) {
			throw new PlaybackException(t);
		}
	}
	
	@Override
	public void setClassPath(File[] arg0) { /* UNUSED */ }
	
	@Override
	public void setFile(String filepath) {
		this.filepath = filepath;
	}
	
	@Override
	public void setVideoFrameParent(Composite frame) {
		if (frame==this.videoFrameParent) return;
		this.videoFrameParent = frame;
		reparentVideo();
	}
	
	@Override
	public void unloadFile() {
		finalisePlayback();
	}
	
	@Override
	public void finalise() { /* UNUSED */ }
	
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
		
		this.m_stopPlaying = false;
		
		if (this.mediaPlayer!=null) {
			this.mediaPlayer.start();
			startWatcherThread();
		}
		
		long l0 = System.currentTimeMillis() - t0;
		System.err.println("Track start time: "+l0+" ms.");
	}
	
	@Override
	public void stopPlaying() throws PlaybackException {
		this.m_stopPlaying = true;
		
		stopWatcherThread();
		if (this.mediaPlayer!=null) {
			this.mediaPlayer.stop();
		}
	}
	
	@Override
	public void pausePlaying() throws PlaybackException {
		if (this.mediaPlayer!=null) {
			this.mediaPlayer.stop();
		}
	}
	
	@Override
	public void resumePlaying() throws PlaybackException {
		if (this.mediaPlayer!=null) {
			this.mediaPlayer.start();
		}
	}
	
	@Override
	public PlayState getPlaybackState() {
		return this.playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		if (this.mediaPlayer!=null) {
			Time mediaTime = this.mediaPlayer.getDuration();
			return (int) Math.round(mediaTime.getSeconds()); // FIXME return long.
		}
		
		return -1;
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		if (this.mediaPlayer!=null) {
			Time mediaTime = this.mediaPlayer.getMediaTime();
			return Math.round(mediaTime.getSeconds());
		}
		
		return -1;
	}
	
	@Override
	public void seekTo(double d) throws PlaybackException {
		if (this.mediaPlayer!=null) {
			Time duration = this.mediaPlayer.getDuration();
			Time target = new Time(duration.getSeconds() * d);
			this.mediaPlayer.setMediaTime(target);
		}
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.

	File mediaFile = null;
	MediaPlayer mediaPlayer = null;
	Composite videoComposite = null;
	Frame videoFrame = null;
	Component videoComponent = null;
	VideoResizeListener videoResizeListener = null;
	
	private void finalisePlayback () {
		if (this.mediaPlayer!=null) {
			this.mediaPlayer.removeControllerListener(this.mediaListener);
			this.mediaPlayer.stop();
			this.mediaPlayer.close();
			this.mediaPlayer = null;
			
			if (this.videoComponent!=null) {
				this.videoFrame.remove(this.videoComponent);
				this.videoFrame.removeComponentListener(this.videoResizeListener);
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
		}
	}
	
	private void _loadTrack () throws PlaybackException {
		callStateListener(PlayState.Loading);
		
		try {
			if (this.mediaPlayer != null) finalisePlayback();
			
			this.mediaFile = new File(this.filepath);
			System.err.println("jmf.PlaybackEngine Creating realized Player...");
			Player player = Manager.createRealizedPlayer(this.mediaFile.toURI().toURL());
			
			System.err.println("jmf.PlaybackEngine Creating MediaPlayer...");
			this.mediaPlayer = new MediaPlayer();
			this.mediaPlayer.setFixedAspectRatio(true);
			this.mediaPlayer.setPlayer(player);
			this.mediaPlayer.setControlPanelVisible(false);
			
			this.mediaPlayer.addControllerListener(this.mediaListener);
			
			reparentVideo();
		}
		catch (Exception e) {
			throw new PlaybackException("Failed to load '"+this.filepath+"'.", e);
		}
	}
	
	private void reparentVideo() {
		System.err.println("jmf.PlaybackEngine >>> reparentVideo()");
		
		if (this.videoFrameParent != null && this.mediaPlayer != null) {
			this.videoComponent = this.mediaPlayer.getVisualComponent();
			
			this.videoFrameParent.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						if (PlaybackEngine.this.videoComponent != null) {
							if (PlaybackEngine.this.videoComposite == null) {
								PlaybackEngine.this.videoComposite = new Composite(PlaybackEngine.this.videoFrameParent, SWT.EMBEDDED);
								PlaybackEngine.this.videoComposite.setLayout(new FillLayout());
								
								PlaybackEngine.this.videoFrame = SWT_AWT.new_Frame(PlaybackEngine.this.videoComposite);
								PlaybackEngine.this.videoFrame.setBackground(Color.BLACK);
								
								PlaybackEngine.this.videoFrame.setLayout(null);
								PlaybackEngine.this.videoFrame.add(PlaybackEngine.this.videoComponent);
								PlaybackEngine.this.videoFrame.doLayout();
								
								PlaybackEngine.this.videoFrameParent.layout();
								
								VideoPanel panelVideo = new VideoPanel(PlaybackEngine.this.mediaPlayer);
								panelVideo.resizeVisualComponent();
								Dimension preferredSize = panelVideo.getPreferredSize();
								PlaybackEngine.this.videoResizeListener = new VideoResizeListener(preferredSize);
								PlaybackEngine.this.videoFrame.addComponentListener(PlaybackEngine.this.videoResizeListener);
								PlaybackEngine.this.videoResizeListener.componentResized(null);
								
								System.err.println("jmf.PlaybackEngine Adding listeners to videoComponent...");
								PlaybackEngine.this.videoComponent.addMouseListener(PlaybackEngine.this.mouseListener);
								PlaybackEngine.this.videoComponent.addKeyListener(PlaybackEngine.this.keyListener);
								
							} else {
								System.err.println("jmf.PlaybackEngine Moveing videoComposite...");
								PlaybackEngine.this.videoComposite.setParent(PlaybackEngine.this.videoFrameParent);
								PlaybackEngine.this.videoFrameParent.layout();
							}
							
						} else {
							System.err.println("jmf.PlaybackEngine videoComponent == null.");
						}
					}
					catch (Exception e) {
						callOnErrorHandler(e);
					}
				}
			});
				
		}
		
		System.err.println("jmf.PlaybackEngine <<< reparentVideo()");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Listeners.
	
	MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			callOnClickListener(e.getButton(), e.getClickCount());
		}
		
		@Override
		public void mouseReleased(MouseEvent e) { /* UNUSED */ }
		@Override
		public void mousePressed(MouseEvent e) { /* UNUSED */ }
		@Override
		public void mouseExited(MouseEvent e) { /* UNUSED */ }
		@Override
		public void mouseEntered(MouseEvent e) { /* UNUSED */ }
	};
	
	KeyListener keyListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent key) {
			callOnKeyPressListener(key.getKeyCode());
		}
		@Override
		public void keyReleased(KeyEvent key) { /* UNUSED */ }
		@Override
		public void keyPressed(KeyEvent key) { /* UNUSED */ }
	};
	
	private class VideoResizeListener implements ComponentListener {
		
		private final Dimension preferredSize;
		
		public VideoResizeListener (Dimension preferredSize) {
			this.preferredSize = preferredSize;
		}
		
		@Override
		public void componentResized(ComponentEvent arg0) {
			if (PlaybackEngine.this.videoFrame!=null && PlaybackEngine.this.videoComponent!=null && this.preferredSize!=null) {
				Dimension dimVideoFrame = PlaybackEngine.this.videoFrame.getSize();
				Rectangle rectVideo = new Rectangle (0, 0, dimVideoFrame.width, dimVideoFrame.height);
				
	            if ((float)this.preferredSize.width/this.preferredSize.height >= (float)dimVideoFrame.width/dimVideoFrame.height) {
	                rectVideo.height = (this.preferredSize.height * dimVideoFrame.width) / this.preferredSize.width;
	                rectVideo.y = (dimVideoFrame.height - rectVideo.height) / 2;
	            } else {
	                rectVideo.width = (this.preferredSize.width * dimVideoFrame.height) / this.preferredSize.height;
	                rectVideo.x = (dimVideoFrame.width - rectVideo.width) / 2;
	            }
	            
	            PlaybackEngine.this.videoComponent.setBounds (rectVideo);
	            PlaybackEngine.this.videoFrame.invalidate();
			}
		}
		
		@Override
		public void componentShown(ComponentEvent arg0) { /* UNUSED */ }
		@Override
		public void componentMoved(ComponentEvent arg0) { /* UNUSED */ }
		@Override
		public void componentHidden(ComponentEvent arg0) { /* UNUSED */ }
	};
	
	private ControllerListener mediaListener = new ControllerListener () {
		@Override
		public void controllerUpdate(ControllerEvent event) {
			
			if (PlaybackEngine.this.mediaPlayer==null) return;
			
			if (event instanceof EndOfMediaEvent) {
				if (!PlaybackEngine.this.m_stopPlaying) {
					callOnEndOfTrackHandler();
				}
				
			} else if (event instanceof StartEvent) {
				callStateListener(PlayState.Playing);
				
			} else if (event instanceof StopEvent) {
				callStateListener(PlayState.Stopped);
				
			} else if (event instanceof FormatChangeEvent) {
				FormatChangeEvent ev = (FormatChangeEvent) event;
				System.err.println(ev.getNewFormat().getEncoding());
				
			} else if (event instanceof ControllerErrorEvent) {
				String message = ((ControllerErrorEvent)event).getMessage();
				callOnErrorHandler(new PlaybackException(message));
				
//			} else if (event instanceof MediaTimeSetEvent) {
//			} else if (event instanceof DurationUpdateEvent) {
//			} else if (event instanceof CachingControlEvent) {
//				if (mediaPlayer.getState() > Controller.Realizing) return;
//				CachingControlEvent ev = (CachingControlEvent) event;
//				CachingControl cc = ev.getCachingControl();
//				cc.getContentLength();
//				cc.getContentProgress();
			}
			
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Watcher thread.
	
	volatile boolean m_stopWatching = false;
	private Thread watcherThread = null;
	
	private void startWatcherThread () {
		this.m_stopWatching = false;
		this.watcherThread = new WatcherThread();
		this.watcherThread.setDaemon(true);
		this.watcherThread.start();
	}
	
	private void stopWatcherThread () {
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
		
		public WatcherThread () {  /* UNUSED */ }

		@Override
		public void run() {
			while (!PlaybackEngine.this.m_stopWatching) {
				
				if (PlaybackEngine.this.mediaPlayer!=null) {
					Time mediaTime = PlaybackEngine.this.mediaPlayer.getMediaTime();
					callPositionListener(Math.round(mediaTime.getSeconds()));
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) { /* UNUSED */ }
			}
		};
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	void callOnErrorHandler (Exception e) {
		if (this.listener!=null) {
			this.listener.onError(e);
		}
	}
	
	void callOnEndOfTrackHandler () {
		if (this.listener!=null) {
			this.listener.onEndOfTrack();
		}
	}
	
	void callStateListener (PlayState state) {
		this.playbackState = state;
		if (this.listener!=null) this.listener.statusChanged(state);
	}
	
	void callPositionListener (long position) {
		if (this.listener!=null) {
			this.listener.positionChanged(position);
		}
	}
	
	// TODO use callOnKeyPressListener
	void callOnKeyPressListener (int keyCode) {
		if (this.listener!=null) {
			this.listener.onKeyPress(keyCode);
		}
	}
	
	// TODO use callOnClickListener
	void callOnClickListener (int button, int clickCount) {
		if (this.listener!=null) {
			this.listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

package net.sparktank.morrigan.playbackimpl.jmf;

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
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings("restriction")
public class PlaybackEngine  implements IPlaybackEngine {
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
		return "net.sparktank.morrigan.playbackimpl.jmf version 0.01.";
	}
	
	@Override
	public String[] getSupportedFormats() {
		return SUPPORTED_FORMATS;
	}
	
	@Override
	public int readFileDuration(String filepath) throws PlaybackException {
		mediaFile = new File(filepath);
		try {
			Player player = Manager.createRealizedPlayer(mediaFile.toURI().toURL());
			Time duration = player.getDuration();
			double seconds = duration.getSeconds();
			player.close();
			
			return (int) seconds;
			
		} catch (Throwable t) {
			throw new PlaybackException(t);
		}
	}
	
	@Override
	public void setClassPath(File[] arg0) {}
	
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
	public void loadTrack() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		_loadTrack();
		long l0 = System.currentTimeMillis() - t0;
		System.err.println("Track load time: "+l0+" ms.");
	}
	
	@Override
	public void startPlaying() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		
		m_stopPlaying = false;
		
		if (mediaPlayer!=null) {
			mediaPlayer.start();
			startWatcherThread();
		}
		
		long l0 = System.currentTimeMillis() - t0;
		System.err.println("Track start time: "+l0+" ms.");
	}
	
	@Override
	public void stopPlaying() throws PlaybackException {
		m_stopPlaying = true;
		
		stopWatcherThread();
		if (mediaPlayer!=null) {
			mediaPlayer.stop();
		}
	}
	
	@Override
	public void pausePlaying() throws PlaybackException {
		if (mediaPlayer!=null) {
			mediaPlayer.stop();
		}
	}
	
	@Override
	public void resumePlaying() throws PlaybackException {
		if (mediaPlayer!=null) {
			mediaPlayer.start();
		}
	}
	
	@Override
	public PlayState getPlaybackState() {
		return playbackState;
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		if (mediaPlayer!=null) {
			Time mediaTime = mediaPlayer.getDuration();
			return (int) Math.round(mediaTime.getSeconds()); // FIXME return long.
		} else {
			return -1;
		}
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		if (mediaPlayer!=null) {
			Time mediaTime = mediaPlayer.getMediaTime();
			return Math.round(mediaTime.getSeconds());
		} else {
			return -1;
		}
	}
	
	public void seekTo(double d) throws PlaybackException {
		if (mediaPlayer!=null) {
			Time duration = mediaPlayer.getDuration();
			Time target = new Time(duration.getSeconds() * d);
			mediaPlayer.setMediaTime(target);
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
		if (mediaPlayer!=null) {
			mediaPlayer.removeControllerListener(mediaListener);
			mediaPlayer.stop();
			mediaPlayer.close();
			mediaPlayer = null;
			
			if (videoComponent!=null) {
				videoFrame.remove(videoComponent);
				videoFrame.removeComponentListener(videoResizeListener);
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
		}
	}
	
	private void _loadTrack () throws PlaybackException {
		callStateListener(PlayState.Loading);
		
		try {
			if (mediaPlayer != null) finalisePlayback();
			
			mediaFile = new File(filepath);
			System.err.println("jmf.PlaybackEngine Creating realized Player...");
			Player player = Manager.createRealizedPlayer(mediaFile.toURI().toURL());
			
			System.err.println("jmf.PlaybackEngine Creating MediaPlayer...");
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setFixedAspectRatio(true);
			mediaPlayer.setPlayer(player);
			mediaPlayer.setControlPanelVisible(false);
			
			mediaPlayer.addControllerListener(mediaListener);
			
			reparentVideo();
		}
		catch (Exception e) {
			throw new PlaybackException("Failed to load '"+filepath+"'.", e);
		}
	}
	
	private void reparentVideo() {
		System.err.println("jmf.PlaybackEngine >>> reparentVideo()");
		
		if (videoFrameParent != null && mediaPlayer != null) {
			videoComponent = mediaPlayer.getVisualComponent();
			
			videoFrameParent.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						if (videoComponent != null) {
							if (videoComposite == null) {
								videoComposite = new Composite(videoFrameParent, SWT.EMBEDDED);
								videoComposite.setLayout(new FillLayout());
								
								videoFrame = SWT_AWT.new_Frame(videoComposite);
								videoFrame.setBackground(Color.BLACK);
								
								videoFrame.setLayout(null);
								videoFrame.add(videoComponent);
								videoFrame.doLayout();
								
								videoFrameParent.layout();
								
								VideoPanel panelVideo = new VideoPanel(mediaPlayer);
								panelVideo.resizeVisualComponent();
								Dimension preferredSize = panelVideo.getPreferredSize();
								videoResizeListener = new VideoResizeListener(preferredSize);
								videoFrame.addComponentListener(videoResizeListener);
								videoResizeListener.componentResized(null);
								
								System.err.println("jmf.PlaybackEngine Adding listeners to videoComponent...");
								videoComponent.addMouseListener(mouseListener);
								videoComponent.addKeyListener(keyListener);
								
							} else {
								System.err.println("jmf.PlaybackEngine Moveing videoComposite...");
								videoComposite.setParent(videoFrameParent);
								videoFrameParent.layout();
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
		public void keyTyped(KeyEvent key) {
			callOnKeyPressListener(key.getKeyCode());
		}
		@Override
		public void keyReleased(KeyEvent key) {}
		@Override
		public void keyPressed(KeyEvent key) {}
	};
	
	private class VideoResizeListener implements ComponentListener {
		
		private final Dimension preferredSize;
		
		public VideoResizeListener (Dimension preferredSize) {
			this.preferredSize = preferredSize;
		}
		
		@Override
		public void componentResized(ComponentEvent arg0) {
			if (videoFrame!=null && videoComponent!=null && preferredSize!=null) {
				Dimension dimVideoFrame = videoFrame.getSize();
				Rectangle rectVideo = new Rectangle (0, 0, dimVideoFrame.width, dimVideoFrame.height);
				
	            if ((float)preferredSize.width/preferredSize.height >= (float)dimVideoFrame.width/dimVideoFrame.height) {
	                rectVideo.height = (preferredSize.height * dimVideoFrame.width) / preferredSize.width;
	                rectVideo.y = (dimVideoFrame.height - rectVideo.height) / 2;
	            } else {
	                rectVideo.width = (preferredSize.width * dimVideoFrame.height) / preferredSize.height;
	                rectVideo.x = (dimVideoFrame.width - rectVideo.width) / 2;
	            }
	            
	            videoComponent.setBounds (rectVideo);
	            videoFrame.invalidate();
			}
		}
		
		@Override
		public void componentShown(ComponentEvent arg0) {}
		@Override
		public void componentMoved(ComponentEvent arg0) {}
		@Override
		public void componentHidden(ComponentEvent arg0) {}
	};
	
	private ControllerListener mediaListener = new ControllerListener () {
		@Override
		public void controllerUpdate(ControllerEvent event) {
			
			if (mediaPlayer==null) return;
			
			if (event instanceof EndOfMediaEvent) {
				if (!m_stopPlaying) {
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
				
				if (mediaPlayer!=null) {
					Time mediaTime = mediaPlayer.getMediaTime();
					callPositionListener(Math.round(mediaTime.getSeconds()));
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
			}
		};
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	private void callOnErrorHandler (Exception e) {
		if (listener!=null) {
			listener.onError(e);
		}
	}
	
	private void callOnEndOfTrackHandler () {
		if (listener!=null) {
			listener.onEndOfTrack();
		}
	}
	
	private void callStateListener (PlayState state) {
		this.playbackState = state;
		if (listener!=null) listener.statusChanged(state);
	}
	
	private void callPositionListener (long position) {
		if (listener!=null) {
			listener.positionChanged(position);
		}
	}
	
	// TODO use callOnKeyPressListener
	private void callOnKeyPressListener (int keyCode) {
		if (listener!=null) {
			listener.onKeyPress(keyCode);
		}
	}
	
	// TODO use callOnClickListener
	private void callOnClickListener (int button, int clickCount) {
		if (listener!=null) {
			listener.onMouseClick(button, clickCount);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

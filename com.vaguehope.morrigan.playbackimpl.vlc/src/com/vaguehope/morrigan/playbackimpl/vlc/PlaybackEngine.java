package com.vaguehope.morrigan.playbackimpl.vlc;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackException;

/**
 * References:
 * https://code.google.com/p/vlcj/wiki/SAQ
 */
public class PlaybackEngine implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final Logger logger = Logger.getLogger(this.getClass().getName());

	private final AtomicBoolean atEos = new AtomicBoolean();
	private final AtomicBoolean stopPlaying = new AtomicBoolean();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	// Property variables (linked to setters & getters).  Denoted by m_.

	private String m_filepath = null;
	protected Composite m_videoParent = null;
	private IPlaybackStatusListener m_listener = null;
	PlayState m_playbackState = PlayState.STOPPED;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.

	public PlaybackEngine () { /* UNUSED */ }

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.

	@Override
	public String getAbout () {
		return "com.vaguehope.morrigan.playbackimpl.gs version 0.01.";
	}

	@Override
	public String[] getSupportedFormats() {
		return Constants.SUPPORTED_FORMATS;
	}

	@Override
	public int readFileDuration(final String fpath) throws PlaybackException {
		return VlcHelper.readFileDuration(fpath);
	}

	@Override
	public void setClassPath(File[] arg0) { /* UNUSED */ }

	@Override
	public void setFile(String filepath) {
		this.m_filepath = filepath;
	}

	@Override
	public void setVideoFrameParent (Composite frame) {
		if (frame != this.m_videoParent) {
			this.m_videoParent = frame;
			reparentVideo(frame, true);
		}
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
		loadFile();
		long l0 = System.currentTimeMillis() - t0;
		this.logger.fine("Track load time: "+l0+" ms.");
	}

	@Override
	public void startPlaying() throws PlaybackException {
		long t0 = System.currentTimeMillis();
		startTrack();
		long l0 = System.currentTimeMillis() - t0;
		this.logger.fine("Track start time: "+l0+" ms.");
	}

	@Override
	public void stopPlaying() throws PlaybackException {
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
		return this.m_playbackState;
	}

	@Override
	public int getDuration() throws PlaybackException {
		EmbeddedMediaPlayer player = this.playerRef.get();
		return (int) (player.getLength() / 1000);
	}

	@Override
	public long getPlaybackProgress() throws PlaybackException {
		EmbeddedMediaPlayer player = this.playerRef.get();
		return (int) (player.getTime() / 1000);
	}

	@Override
	public void seekTo(double d) throws PlaybackException {
		this.playLock.lock();
		try {
			EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				player.setPosition((float) d);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.m_listener = listener;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local play-back methods.

	private Lock playLock = new ReentrantLock();
	protected AtomicReference<EmbeddedMediaPlayer> playerRef = new AtomicReference<EmbeddedMediaPlayer>();

	Composite videoComposite = null; // Goes between parent and frame.
	Frame videoFrame = null; // Created by SWT_AWT.

	protected AtomicBoolean hasVideo = new AtomicBoolean(false);

	private void finalisePlayback () {
		this.logger.entering(this.getClass().getName(), "finalisePlayback");

		this.playLock.lock();
		try {
			EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				player.stop();
				player.release();
				this.playerRef.set(null);
			}
			reparentVideo(null, false);
		}
		finally {
			this.playLock.unlock();
		}

		this.logger.exiting(this.getClass().getName(), "finalisePlayback");
	}

	private void loadFile () throws PlaybackException {
		this.logger.entering(this.getClass().getName(), "_loadTrack");

		this.playLock.lock();
		try {
			setStateAndCallListener(PlayState.LOADING);
			EmbeddedMediaPlayer player = this.playerRef.get();
			this.logger.fine("firstLoad=" + (player == null));
			if (player == null) {
				this.logger.fine("About to create player object...");
				player = Activator.getFactory().newEmbeddedMediaPlayer();
				this.playerRef.set(player);

				this.logger.fine("Connecting event listener...");
				player.addMediaPlayerEventListener(this.mediaEventListener);
			}
			else {
				player.stop();
			}

			this.logger.fine("About to set input file to '" + this.m_filepath + "'...");
			player.prepareMedia(new File(this.m_filepath).getAbsolutePath());
			this.logger.fine("Input file set.");

			this.hasVideo.set(FormatHelper.mightFileHaveVideo(this.m_filepath));
			this.logger.fine("this.hasVideo=" + this.hasVideo.get());
			reparentVideo(this.hasVideo.get() ? this.m_videoParent : null, false);
		}
		catch (Exception e) { // NOSONAR Report any error while loading files.
			setStateAndCallListener(PlayState.STOPPED);
			throw new PlaybackException("Failed to load '"+this.m_filepath+"'.", e);
		}
		finally {
			this.playLock.unlock();
		}

		this.logger.exiting(this.getClass().getName(), "_loadTrack");
	}

	@SuppressWarnings("boxing")
	protected void reparentVideo (final Composite newParent, final boolean seek) {
		this.logger.entering(this.getClass().getName(), "reparentVideo", new Object[] { newParent, seek } );

		this.playLock.lock();
		try {
			// Clear old values but keep references so we can dispose them later.
			final Composite old_videoComposite = this.videoComposite;
			this.videoComposite = null;
			final Frame old_videoFrame = this.videoFrame;
			this.videoFrame = null;

			final EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				if (newParent != null && this.hasVideo.get()) { // Do we have any video to attach and somewhere to put it?
					this.logger.fine("Creating new video frame...");
					ThreadHelper.runInUiThread(newParent, new Runnable() {
						@Override
						public void run() {
							Control[] children = newParent.getChildren();
							for (Control control : children) {
								PlaybackEngine.this.logger.warning("Video parent already has child: " + control);
							}

							Composite comp = new Composite(newParent, SWT.EMBEDDED);
							comp.setLayout(new FillLayout());

							Frame frame = SWT_AWT.new_Frame(comp); // REMEMBER: can only do this once per Composite.
							frame.setVisible(true);
							newParent.layout();

							Canvas canvas = new Canvas();
							canvas.setBackground(java.awt.Color.black);
							frame.add(canvas, BorderLayout.CENTER);

							// So far as I know the video surface does not require disposal.
							CanvasVideoSurface videoSurface = Activator.getFactory().newVideoSurface(canvas);
							player.setVideoSurface(videoSurface);

							// If video is playing, put it back again...
							if (seek && (player.isPlaying() || getPlaybackState() == PlayState.PAUSED )) {
								PlaybackEngine.this.logger.fine("Restarting playback...");
								long time = player.getTime();
								player.stop();
								player.play(); // This is an async call.
								player.setTime(time);
							}

							canvas.addKeyListener(PlaybackEngine.this.keyListener);
							canvas.addMouseListener(PlaybackEngine.this.mouseListener);

							PlaybackEngine.this.videoComposite = comp;
							PlaybackEngine.this.videoFrame = frame;
						}
					});
				}
				else { // If there is no video or nowhere to put video...
					this.logger.fine("setting null video surface...");
					player.setVideoSurface(null);
				}
			}

			// If we left stuff that needed disposing, do so here.
			this.logger.fine("cleanup...");
			if (old_videoFrame != null) old_videoFrame.dispose();
			if (old_videoComposite != null && !old_videoComposite.isDisposed()) {
				ThreadHelper.runInUiThread(old_videoComposite, new Runnable() {
					@Override
					public void run () {
						Composite parent = old_videoComposite.getParent();
						old_videoComposite.dispose();
						if (!parent.isDisposed()) parent.layout();
					}
				});
			}
		}
		finally {
			this.playLock.unlock();
		}
		this.logger.exiting(this.getClass().getName(), "reparentVideo");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Event handlers.

	private MediaPlayerEventListener mediaEventListener = new MediaPlayerEventAdapter () {

		@Override
		public void finished(MediaPlayer mediaPlayer) {
			PlaybackEngine.this.logger.entering(this.getClass().getName(), "finished");
			if (mediaPlayer == PlaybackEngine.this.playerRef.get()) handleEosEvent("g");
			PlaybackEngine.this.logger.exiting(this.getClass().getName(), "finished");
		}

		@Override
		public void stopped(MediaPlayer mediaPlayer) {
			setStateAndCallListener(PlayState.STOPPED);
		}
		@Override
		public void playing(MediaPlayer mediaPlayer) {
			setStateAndCallListener(PlayState.PLAYING);
		}
		@Override
		public void paused(MediaPlayer mediaPlayer) {
			setStateAndCallListener(PlayState.PAUSED);
		}

		@Override
		public void timeChanged (MediaPlayer mediaPlayer, long newTime) {
			callPositionListener(newTime / 1000);
		}

		@Override
		public void lengthChanged (MediaPlayer mediaPlayer, long newLength) {
			callDurationListener((int) (newLength / 1000));
		}

		@Override
		public void error (MediaPlayer mediaPlayer) {
			// TODO work out what to do with this.  call m_listener ?
		}

	};

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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Control methods.

	private void startTrack () {
		this.logger.entering(this.getClass().getName(), "_startTrack");

		this.playLock.lock();
		try {
			this.stopPlaying.set(false);
			this.atEos.set(false);

			EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				this.logger.fine("calling playbin.setState(PLAYING)...");
				player.play(); // This is an async call.
//				setStateAndCallListener(PlayState.Playing); // Do think this is needed as there will be a call back.
			}
		}
		finally {
			this.playLock.unlock();
		}

		this.logger.exiting(this.getClass().getName(), "_startTrack");
	}

	private void pauseTrack () {
		this.playLock.lock();
		try {
			EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				player.setPause(true);
				setStateAndCallListener(PlayState.PAUSED);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	private void resumeTrack () {
		this.playLock.lock();
		try {
			EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				/* Using play() instead of setPause(false) as it seems safer.
				 * May change it if it causes issues.
				 */
				player.play();
				setStateAndCallListener(PlayState.PLAYING);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	private void stopTrack () {
		this.playLock.lock();
		try {
			this.stopPlaying.set(true);
			EmbeddedMediaPlayer player = this.playerRef.get();
			if (player != null) {
				player.stop();
				setStateAndCallListener(PlayState.STOPPED);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

	void handleEosEvent (String debugType) {
		this.logger.entering(this.getClass().getName(), "handleEosEvent", "(type="+debugType+",m_stopPlaying="+this.stopPlaying.get()+",m_atEos="+this.atEos.get()+")");

		if (!this.stopPlaying.get()) {
			if (this.atEos.compareAndSet(false, true)) {
				callOnEndOfTrackHandler();
			}
		}

		this.logger.exiting(this.getClass().getName(), "handleEosEvent");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Listener helper methods.

	private void callOnEndOfTrackHandler () {
		setStateAndCallListener(PlayState.STOPPED);
		if (this.m_listener!=null) this.m_listener.onEndOfTrack();
	}

	void setStateAndCallListener (PlayState state) {
		this.m_playbackState = state;
		if (this.m_listener != null) this.m_listener.statusChanged(state);
	}

	void callPositionListener(long position) {
		if (this.m_listener != null) this.m_listener.positionChanged(position);
		if (this.hasVideo.get()) ScreenSaver.poke();
	}

	void callDurationListener(int duration) {
		if (this.m_listener != null) this.m_listener.durationChanged(duration);
	}

	void callOnKeyPressListener(int keyCode) {
		if (this.m_listener != null) this.m_listener.onKeyPress(keyCode);
	}

	void callOnClickListener(int button, int clickCount) {
		if (this.m_listener != null) this.m_listener.onMouseClick(button, clickCount);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

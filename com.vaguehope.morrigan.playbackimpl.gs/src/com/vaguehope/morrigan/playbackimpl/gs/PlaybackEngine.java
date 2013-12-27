package com.vaguehope.morrigan.playbackimpl.gs;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Format;
import org.gstreamer.GstObject;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.elements.PlayBin;
import org.gstreamer.swt.overlay.VideoComponent;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine;
import com.vaguehope.morrigan.engines.playback.IPlaybackStatusListener;
import com.vaguehope.morrigan.engines.playback.PlaybackException;

/* Main play-back class:
 * http://www.humatic.de/htools/dsj/javadoc/de/humatic/dsj/DSFiltergraph.html
 *
 * Event constants:
 * http://www.humatic.de/htools/dsj/javadoc/constant-values.html
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
		return GStreamerHelper.readFileDuration(fpath);
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
		return (int) this.playbin.queryDuration(TimeUnit.SECONDS);
	}

	@Override
	public long getPlaybackProgress() throws PlaybackException {
		return this.playbin.queryPosition(TimeUnit.SECONDS);
	}

	@Override
	public void seekTo(double d) throws PlaybackException {
		this.playLock.lock();
		try {
			if (this.playbin != null) {
				long duration = this.playbin.queryDuration(TimeUnit.NANOSECONDS);
				this.playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, (long) (d * duration), SeekType.NONE, -1);
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
	protected PlayBin playbin = null; // TODO replace with atomic reference.

	protected VideoComponent videoComponent = null; // SWT / GStreamer.
	protected Element videoElement = null;          // GStreamer.
	protected AtomicBoolean hasVideo = new AtomicBoolean(false);

	private void finalisePlayback () {
		this.logger.entering(this.getClass().getName(), "finalisePlayback");

		this.playLock.lock();
		try {
			stopWatcherThread();
			if (this.playbin != null) {
				this.playbin.setState(State.NULL);
				this.playbin.dispose();
				this.playbin = null;
			}
			reparentVideo(null, false); // Since playbin is null this should just clean up.
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
			boolean firstLoad = (this.playbin == null);
			this.logger.fine("firstLoad=" + firstLoad);
			if (firstLoad) {
				this.logger.fine("About to create PlayBin object...");
				this.playbin = new PlayBin("VideoPlayer");

				this.logger.fine("Connecting eosBus...");
				this.playbin.getBus().connect(this.eosBus);
				this.logger.fine("Connecting stateChangedBus...");
				this.playbin.getBus().connect(this.stateChangedBus);
			}
			else {
				this.playbin.setState(State.NULL);
			}

			this.hasVideo.set(this.m_videoParent == null ? false : FormatHelper.mightFileHaveVideo(this.m_filepath));
			reparentVideo(this.hasVideo.get() ? this.m_videoParent : null, false);

			this.logger.fine("About to set input file to '" + this.m_filepath + "'...");
			this.playbin.setInputFile(new File(this.m_filepath));
			this.logger.fine("Input file set.");
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
			// Clean up any stuff we had before.
			if (this.videoComponent != null) {
				if (!this.videoComponent.isDisposed()) {
					this.logger.fine("Removing listeners...");
					ThreadHelper.runInUiThread(this.videoComponent, new Runnable() {
						@Override
						public void run() {
							PlaybackEngine.this.videoComponent.removeKeyListener(PlaybackEngine.this.keyListener);
							PlaybackEngine.this.videoComponent.removeMouseListener(PlaybackEngine.this.mouseListener);
							PlaybackEngine.this.logger.fine("listeners removed.");
						}
					});
				}
			}

			// Clear old values but keep references so we can dispose them later.
			final VideoComponent old_videoComponent = this.videoComponent;
			this.videoComponent = null;
			final Element old_videoElement = this.videoElement;
			this.videoElement = null;

			if (this.playbin != null) { // Do we have anything to attach video output to?
				if (this.hasVideo.get() && newParent != null) { // Can not attach video to something that is not there...
					/* We can not move the video while it is playing, so if it is,
					 * stop it and remember where it was.
					 */
					long position = -1;
					this.logger.fine("checking current state...");
					State state = this.playbin.getState();
					this.logger.fine("state = " + state + ".");
					if (state == State.PLAYING || state == State.PAUSED) {
						this.logger.fine("stopping playback...");
						position = this.playbin.queryPosition(TimeUnit.NANOSECONDS);
						this.logger.fine("position=" + position);
						this.playbin.setState(State.NULL);
					}

					this.logger.fine("Creating new VideoComponent...");
					ThreadHelper.runInUiThread(newParent, new Runnable() {
						@Override
						public void run() {
							PlaybackEngine.this.videoComponent = new VideoComponent(newParent, SWT.NO_BACKGROUND);
							PlaybackEngine.this.videoComponent.setKeepAspect(true);
							PlaybackEngine.this.videoComponent.expose(); // This is important on Ubuntu 10.10 (not needed on 10.04).
							PlaybackEngine.this.videoElement = PlaybackEngine.this.videoComponent.getElement();
							PlaybackEngine.this.playbin.setVideoSink(PlaybackEngine.this.videoElement);
							newParent.layout();

							PlaybackEngine.this.videoComponent.addKeyListener(PlaybackEngine.this.keyListener);
							PlaybackEngine.this.videoComponent.addMouseListener(PlaybackEngine.this.mouseListener);
						}
					});

					// If video was playing, put it back again...
					if (seek && position >= 0) {
						this.playbin.setState(State.PAUSED);
						if (position > 0) {
							long startTime = System.currentTimeMillis();
							while (true) {
								this.playbin.seek(1.0d, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, position, SeekType.NONE, -1);
								if (this.playbin.queryPosition(TimeUnit.NANOSECONDS) > 0) { break; }
								try { Thread.sleep(200); } catch (InterruptedException e) { /* UNUSED */ }
								if (this.playbin.queryPosition(TimeUnit.NANOSECONDS) > 0) { break; }
							}

							this.logger.fine("Seek took " + (System.currentTimeMillis() - startTime) + " ms.");
						}

						if (state != State.PAUSED) this.playbin.setState(state);
					}
				}
				else { // If there is no video or nowhere to put video...
					this.logger.fine("setVideoSink(fakesink)...");
					Element fakesink = ElementFactory.make("fakesink", "videosink");
					fakesink.set("sync", Boolean.TRUE);
					this.playbin.setVideoSink(fakesink);
					PlaybackEngine.this.videoElement = fakesink; // So we clean it up correctly.
				}
			}

			// If we left stuff that needed disposing, do so here.
			this.logger.fine("cleanup...");
			if (old_videoElement != null) old_videoElement.dispose();
			if (old_videoComponent != null && !old_videoComponent.isDisposed()) {
				ThreadHelper.runInUiThread(old_videoComponent, new Runnable() {
					@Override
					public void run() {
						Composite parent = old_videoComponent.getParent();
						old_videoComponent.dispose();
						if (!parent.isDisposed()) parent.layout();
					}
				});
			}
		}
		finally {
			this.playLock.unlock();
		}
		this.logger.exiting(this.getClass().getName(), "reparentVideo", Boolean.TRUE);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Event handlers.

	private Bus.EOS eosBus = new Bus.EOS() {
		@Override
		public void endOfStream(GstObject source) {
			PlaybackEngine.this.logger.entering(this.getClass().getName(), "endOfStream");

			if (source == PlaybackEngine.this.playbin) {
				handleEosEvent("g");
			}

			PlaybackEngine.this.logger.exiting(this.getClass().getName(), "endOfStream");
		}
	};

	private Bus.STATE_CHANGED stateChangedBus = new Bus.STATE_CHANGED() {
		@Override
		public void stateChanged(GstObject source, State old, State current, State pending) {
			if (source == PlaybackEngine.this.playbin) {
				switch (current) {
				case NULL:
					setStateAndCallListener(PlayState.STOPPED);
					break;

				case PLAYING:
					setStateAndCallListener(PlayState.PLAYING);
					break;

				case PAUSED:
					setStateAndCallListener(PlayState.PAUSED);
					break;

				case READY:
					setStateAndCallListener(PlayState.STOPPED); // TODO add "Loaded" to enum?
					break;

				}
			}
		}
	};

	KeyListener keyListener = new KeyListener() {
		@Override
		public void keyReleased(KeyEvent key) {
			callOnKeyPressListener(key.keyCode);
		}
		@Override
		public void keyPressed(KeyEvent arg0) { /* UNUSED */ }
	};

	MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			callOnClickListener(e.button, e.count);
		}
		@Override
		public void mouseUp(MouseEvent arg0) { /* UNUSED */ }
		@Override
		public void mouseDown(MouseEvent arg0) { /* UNUSED */ }
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Control methods.

	private void startTrack () {
		this.logger.entering(this.getClass().getName(), "_startTrack");

		this.playLock.lock();
		try {
			this.stopPlaying.set(false);
			this.atEos.set(false);

			if (this.playbin != null) {
				startWatcherThread();

				this.logger.fine("calling playbin.setState(PLAYING)...");
				this.playbin.setState(State.PLAYING);
				setStateAndCallListener(PlayState.PLAYING);

				if (this.hasVideo.get()) {
					// FIXME How to stop more than one of these starting?
					WaitForVideoThread t = new WaitForVideoThread(this.playbin, new Runnable() {
						@Override
						public void run() {
							PlaybackEngine.this.hasVideo.set(false);
							reparentVideo(null, true);
						}
					});
					t.start();
				}
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
			if (this.playbin!=null) {
				this.playbin.setState(State.PAUSED);
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
			if (this.playbin != null) {
				this.playbin.setState(State.PLAYING);
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
			stopWatcherThread();
			if (this.playbin != null) {
				this.playbin.setState(State.NULL);
				setStateAndCallListener(PlayState.STOPPED);
			}
		}
		finally {
			this.playLock.unlock();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Lock watchThreadLock = new ReentrantLock();
	private final AtomicReference<WatcherThread> watchThread = new AtomicReference<WatcherThread>(null);

	private void startWatcherThread () {
		this.logger.entering(this.getClass().getName(), "startWatcherThread");

		this.watchThreadLock.lock();
		try {
			if (this.watchThread.get() != null) {
				this.logger.warning("having to stop watcher thread from startWatcherThread().");
				stopWatcherThread();
			}

			WatcherThread t = new WatcherThread(this);
			t.setDaemon(true);
			t.start();
			this.watchThread.set(t);
		}
		finally {
			this.watchThreadLock.unlock();
		}

		this.logger.exiting(this.getClass().getName(), "startWatcherThread");
	}

	private void stopWatcherThread () {
		this.logger.entering(this.getClass().getName(), "stopWatcherThread");

		this.watchThreadLock.lock();
		try {
			WatcherThread t = this.watchThread.getAndSet(null);
			if (t != null) {
				t.stopWatching();
				if (t.isAlive()) {
					t.interrupt();
					try {
						if (!Thread.currentThread().equals(t)) t.join();
					}
					catch (InterruptedException e) {
						this.logger.log(Level.WARNING, "Interupted waiting for old watchThread to stop.", e);
					}
				}
			}
		}
		finally {
			this.watchThreadLock.unlock();
		}

		this.logger.exiting(this.getClass().getName(), "stopWatcherThread");
	}

	void handleEosEvent (String debugType) {
		this.logger.entering(this.getClass().getName(), "handleEosEvent", "(type="+debugType+",m_stopPlaying="+this.stopPlaying.get()+",m_atEos="+this.atEos.get()+")");

		stopWatcherThread();

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

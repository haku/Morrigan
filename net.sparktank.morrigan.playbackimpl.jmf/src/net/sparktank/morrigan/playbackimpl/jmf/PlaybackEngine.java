package net.sparktank.morrigan.playbackimpl.jmf;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.media.CannotRealizeException;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;

import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.NotImplementedException;
import net.sparktank.morrigan.playback.PlaybackException;

@SuppressWarnings("restriction")
public class PlaybackEngine  implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO Any more?
	private final static String[] SUPPORTED_FORMATS = {"wav", "mp3"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private volatile boolean m_stopPlaying;
	
	private String filepath = null;
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
	public void setFile(String filepath) {
		this.filepath = filepath;
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
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.

	File mediaFile = null;
	Player mediaPlayer = null;
	
	private void finalisePlayback () {
		if (mediaPlayer!=null) {
			mediaPlayer.removeControllerListener(mediaListener);
			mediaPlayer.stop();
			mediaPlayer.close();
			mediaPlayer.deallocate();
			mediaPlayer = null;
		}
	}
	
	private void loadTrack () throws NoPlayerException, CannotRealizeException, MalformedURLException, IOException {
		mediaFile = new File(filepath);
		mediaPlayer = Manager.createRealizedPlayer(mediaFile.toURI().toURL());
		mediaPlayer.addControllerListener(mediaListener);
	}
	
	private void playTrack () {
		if (mediaPlayer!=null) {
			mediaPlayer.start();
			startWatcherThread();
		}
	}
	
	private void stopTrack () {
		stopWatcherThread();
		if (mediaPlayer!=null) {
			mediaPlayer.stop();
		}
	}
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

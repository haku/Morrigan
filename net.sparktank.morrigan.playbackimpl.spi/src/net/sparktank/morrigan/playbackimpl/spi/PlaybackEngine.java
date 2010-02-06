package net.sparktank.morrigan.playbackimpl.spi;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.sparktank.morrigan.playback.IPlaybackEngine;
import net.sparktank.morrigan.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.playback.NotImplementedException;
import net.sparktank.morrigan.playback.PlaybackException;

public class PlaybackEngine implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO Any more?
	private final static String[] SUPPORTED_FORMATS = {"wav", "mp3", "ogg"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private volatile boolean m_stopPlaying;
	
	private String filepath = null;
	private Runnable onFinishHandler = null;
	private IPlaybackStatusListener listener = null;
	private PlayState playbackState = PlayState.Stopped;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.

	public PlaybackEngine () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.
	
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
			throw new PlaybackException(e);
		}
		
		startPlaybackThread();
	}
	
	@Override
	public void stopPlaying() throws PlaybackException {
		stopPlaybackThread();
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
		return -1;
	}
	
	@Override
	public long getPlaybackProgress() throws PlaybackException {
		return -1;
	}
	
	@Override
	public void setStatusListener(IPlaybackStatusListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void setOnfinishHandler(Runnable runnable) {
		this.onFinishHandler = runnable;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Thread stuff.
	
	Thread playThread = null;
	
	private void startPlaybackThread () {
		playThread = new Thread() {
			@Override
			public void run() {
				super.run();
				
				try {
					playTrack();
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		        
			     // If not a requested stop, triger next event.
		        if (!m_stopPlaying) {
		        	callOnFinishHandler();
		        }
			}
		};
		
		playThread.setDaemon(true);
		playThread.start();
	}
	
	private void stopPlaybackThread () {
		m_stopPlaying = true;
		try {
			if (playThread!=null
					&& playThread.isAlive()
					&& !Thread.currentThread().equals(playThread)) {
				playThread.join();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local playback methods.
	
	AudioInputStream in = null;
	AudioInputStream din = null;
	AudioFormat decodedFormat = null;

	private void finalisePlayback () {
		try {
			if (in!=null) in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (din!=null) din.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadTrack () throws UnsupportedAudioFileException, IOException {
		File file = new File(filepath);
		in = AudioSystem.getAudioInputStream(file);
        din = null;
        AudioFormat baseFormat = in.getFormat();
        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false);
        din = AudioSystem.getAudioInputStream(decodedFormat, in);
	}
	
	private void playTrack () throws IOException, LineUnavailableException {
		// Play now.
		callStateListener(PlayState.Playing);
		
        rawplay(decodedFormat, din); // Blocks during playback.
        in.close();
        
        callStateListener(PlayState.Stopped);
	}
	
	private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
		byte[] data = new byte[4096];
		SourceDataLine line = null;
		
		try {
			line = getLine(targetFormat);
			if (line != null) {
				// Start
				line.start();
				@SuppressWarnings("unused") // TODO use nBytesWritten???
				int nBytesRead = 0, nBytesWritten = 0;
				while ((nBytesRead != -1) && (!m_stopPlaying)) {
					nBytesRead = din.read(data, 0, data.length);
					if (nBytesRead != -1) {
						nBytesWritten = line.write(data, 0, nBytesRead);
					}
					
					callPositionListener(line.getMicrosecondPosition()/1000000);
				}
			}
			
		} finally {
			if (line!=null) {
				line.drain();
				line.stop();
				line.close();
			}
			din.close();
		}
	}
	
	private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
		SourceDataLine res = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
		res = (SourceDataLine) AudioSystem.getLine(info);
		res.open(audioFormat);
		return res;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Local helper methods.
	
	private void callOnFinishHandler () {
		System.out.println("callOnFinishHandler >>");
		if (onFinishHandler!=null) onFinishHandler.run();
		System.out.println("callOnFinishHandler <<");
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
}

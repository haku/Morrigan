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
import net.sparktank.morrigan.playback.NotImplementedException;
import net.sparktank.morrigan.playback.PlaybackException;

public class PlaybackEngine implements IPlaybackEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private boolean m_stopPlaying;
	private Runnable onFinishHandler;
	private String filepath;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.

	public PlaybackEngine () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IPlaybackEngine methods.
	
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
		m_stopPlaying = true;
	}
	
	@Override
	public void pausePlaying() throws PlaybackException {
		throw new NotImplementedException();
	}
	
	@Override
	public int getDuration() throws PlaybackException {
		return -1;
	}

	@Override
	public int getPlaybackProgress() throws PlaybackException {
		return -1;
	}

	@Override
	public void setOnfinishHandler(Runnable runnable) {
		this.onFinishHandler = runnable;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Thread stuff.
	
	private void startPlaybackThread () {
		Thread t = new Thread() {
			@Override
			public void run() {
				super.run();
				
				try {
					playTrack();
		            
		        } catch (Exception ex) {
		            System.out.println(ex);
		            callOnFinishHandler();
		        }
			}
		};
		t.setDaemon(true);
		t.start();
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
        rawplay(decodedFormat, din); // Blocks during playback.
        in.close();
        
        // If stopPlaying is true, something has requested the stop and we'll
        // leave it up to that to decide what to do (e.g. next, prev, pause).
        // If we've reached here and stopPlaying is false, we naturally got
        // to the end of the song, so fire a next.
        if (!m_stopPlaying) {
        	callOnFinishHandler();
        }
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
		if (onFinishHandler!=null) onFinishHandler.run();
	}
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

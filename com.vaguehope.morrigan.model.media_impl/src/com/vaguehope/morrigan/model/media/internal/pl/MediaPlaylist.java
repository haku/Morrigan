package com.vaguehope.morrigan.model.media.internal.pl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.playback.NotImplementedException;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaPlaylist;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.internal.MediaItemList;
import com.vaguehope.morrigan.model.media.internal.MediaTrackListHelper;


public class MediaPlaylist extends MediaItemList<IMediaTrack> implements IMediaPlaylist {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private boolean newPl = false;
	private boolean alreadyRead = false;
	private String filePath = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaPlaylist(String title, String filePath) {
		super(filePath, title);
		this.filePath = filePath;
	}
	
	public MediaPlaylist(String title, String filePath, boolean newPl) {
		super(filePath, title);
		this.filePath = filePath;
		this.newPl = newPl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void finalize() throws Throwable {
		try {
			clean();
			
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * If there is only metadata to save, go ahead and save it.
	 * @throws MorriganException 
	 */
	public void clean () throws MorriganException {
		if (getDirtyState() == DirtyState.METADATA) {
			writeToFile();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@Override
	public String getSerial() {
		return this.filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean isCanBeDirty () {
		return true;
	}
	
	@Override
	public boolean allowDuplicateEntries () {
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private long durationOfLastRead = -1;
	
	@Override
	public void read () throws MorriganException {
		long t0 = System.currentTimeMillis();
		
		if (this.newPl) {
			if (new File(this.filePath).exists()) {
				throw new MorriganException("Play list already exists.");
			}
			writeToFile();
			this.newPl = false;
		}
		else if (!this.alreadyRead) {
			loadFromFile();
			this.alreadyRead = true;
		}
		
		long l0 = System.currentTimeMillis() - t0;
		this.durationOfLastRead = l0;
	}
	
	@Override
	public long getDurationOfLastRead() {
		return this.durationOfLastRead;
	}
	
	@Override
	public void forceRead() throws MorriganException {
		throw new IllegalArgumentException("Don't do this.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getFilePath () {
		return this.filePath;
	}
	
	@Override
	public void addNewItem(String filepath) {
		addItem(new MediaPlaylistTrack(filepath));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	The playlist file format.
	
	/*
	 * #file=<path>|<hash>|<starts>|<ends>|<duration>|<date last played>
	 * <durtion> = seconds.
	 * <date last played> = yyyy-mm-dd-hh-mm-ss.
	 */
	
	private static final String PL_ITEM_IDENTIFIER = "#file=";
	
	private SimpleDateFormat PL_DATE = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	
	@Override
	public void loadFromFile () throws MorriganException {
		this.logger.fine("Reading PlayList from '" + this.filePath + "'...");
		
		File file = new File(this.filePath);
        BufferedReader reader = null;
        
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new MorriganException("Failed to open play list file for reading.", e);
		}
		
		// repeat until all lines is read
		String text = null;
		try {
			int n = 0;
			while ((text = reader.readLine()) != null) {
				
				if (text.startsWith(PL_ITEM_IDENTIFIER)) {
					String[] line = text.substring(PL_ITEM_IDENTIFIER.length()).split("\\|");
					IMediaTrack item = new MediaPlaylistTrack(line[0]);
					
//					if (line.length>=2 && line[1].length()>0) {
//						// TODO set hash for item = item[1].
//					}
					
					if (line.length>=3 && line[2].length()>0) {
						item.setStartCount(Long.parseLong(line[2]));
					}
					
					if (line.length>=4 && line[3].length()>0) {
						item.setEndCount(Long.parseLong(line[3]));
					}
					
//					if (line.length>=5 && line[4].length()>0) {
//						// TODO set duration for item.
//					}
					
					if (line.length>=6 && line[5].length()>0) {
						try {
							Date date = this.PL_DATE.parse(line[5]);
							item.setDateLastPlayed(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					
					addItem(item);
					n++;
				}
				
			}
			this.logger.fine("Read " + n + " lines from '" + this.filePath + "'.");
			
		} catch (IOException e) {
			throw new MorriganException("Error while reading play list.", e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				throw new MorriganException("Failed to close file handle.", e);
			}
		}
		
		setDirtyState(DirtyState.CLEAN);
	}
	
	@Override
	public void writeToFile () throws MorriganException {
		this.logger.fine("Writing PlayList to '" + this.filePath + "'...");
		
		File file = new File(this.filePath);
        Writer writer = null;
        
        try {
        	writer = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			throw new MorriganException("Failed to open file to write to.", e);
		}
        
		try {
			int n = 0;
			for (IMediaTrack mt : getMediaItems()) {
				
				writer.write(
						PL_ITEM_IDENTIFIER + mt.getFilepath()
						+ "|0" // TODO hashcode.
						+ "|" + mt.getStartCount()
						+ "|" + mt.getEndCount()
						+ "|0" // TODO duration.
						+ "|" + (mt.getDateLastPlayed()==null ? "" : this.PL_DATE.format(mt.getDateLastPlayed()))
						+ "\n");
				
				writer.write(mt.getFilepath() + "\n");
				n ++;
			}
			this.logger.fine("Wrote " + n + " lines to '" + this.filePath + "'.");
			
		} catch (IOException e) {
			throw new MorriganException("Error while write play list to file.", e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				throw new MorriganException("Failed to close file handle.", e);
			}
		}
		
		setDirtyState(DirtyState.CLEAN);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Update methods.  Use these for data that is to be persisted.
//	These methods are sub-classed where persistence is needed.
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackStartCnt (IMediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track, n);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackEndCnt (IMediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track, n);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackStartCnt (IMediaTrack track) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackEndCnt (IMediaTrack track) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track);
	}
	
	@Override
	public void setTrackStartCnt(IMediaTrack item, long n) throws MorriganException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}
	
	@Override
	public void setTrackEndCnt(IMediaTrack item, long n) throws MorriganException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}
	
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setTrackDuration (IMediaTrack track, int duration) throws MorriganException {
		MediaTrackListHelper.setTrackDuration(this, track, duration);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setTrackDateLastPlayed (IMediaTrack track, Date date) throws MorriganException {
		MediaTrackListHelper.setDateLastPlayed(this, track, date);
	}
	
	@Override
	public void setRemoteLocation(IMediaTrack track, String remoteLocation) throws MorriganException {
		throw new NotImplementedException();
	}
	
	@Override
	public void persistTrackData(IMediaTrack track) throws MorriganException {
		throw new NotImplementedException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata readers.
	
	@Override
	public DurationData getTotalDuration () {
		return MediaTrackListHelper.getTotalDuration(this.getMediaItems());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		MediaPlaylist other = (MediaPlaylist) obj;
		if (this.filePath == null) {
			if (other.filePath != null) {
				return false;
			}
		} else if (!this.filePath.equals(other.filePath)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		// Since equals() only uses filePath, we can do this.
		return this.filePath.hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
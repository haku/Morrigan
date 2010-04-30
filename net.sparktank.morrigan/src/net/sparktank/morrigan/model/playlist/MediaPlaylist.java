package net.sparktank.morrigan.model.playlist;

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

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList;

public class MediaPlaylist extends MediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "PLAYLIST";
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private boolean newPl = false;
	private boolean alreadyRead = false;
	private String filePath = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaPlaylist(String title, String filePath) throws MorriganException {
		super(filePath, title);
		this.filePath = filePath;
	}
	
	public MediaPlaylist(String title, String filePath, boolean newPl) throws MorriganException {
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
		if (getDirtyState()==DirtyState.METADATA) {
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
		return filePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isCanBeDirty () {
		return true;
	}
	
	public boolean allowDuplicateEntries () {
		return true;
	}
	
	@Override
	public void read () throws MorriganException {
		if (newPl) {
			if (new File(filePath).exists()) {
				throw new MorriganException("Play list already exists.");
			}
			writeToFile();
			newPl = false;
		} else if (!alreadyRead) {
			loadFromFile();
			alreadyRead = true;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String getFilePath () {
		return filePath;
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
	
	public void loadFromFile () throws MorriganException {
		logger.fine("Reading PlayList from '" + filePath + "'...");
		
		File file = new File(filePath);
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
					MediaItem item = new MediaItem(line[0]);
					
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
							Date date = PL_DATE.parse(line[5]);
							item.setDateLastPlayed(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					
					addTrack(item);
					n++;
				}
				
			}
			logger.fine("Read " + n + " lines from '" + filePath + "'.");
			
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
	
	public void writeToFile () throws MorriganException {
		logger.fine("Writing PlayList to '" + filePath + "'...");
		
		File file = new File(filePath);
        Writer writer = null;
        
        try {
        	writer = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			throw new MorriganException("Failed to open file to write to.", e);
		}
        
		try {
			int n = 0;
			for (MediaItem mt : getMediaTracks()) {
				
				writer.write(
						PL_ITEM_IDENTIFIER + mt.getFilepath()
						+ "|0" // TODO hashcode.
						+ "|" + mt.getStartCount()
						+ "|" + mt.getEndCount()
						+ "|0" // TODO duration.
						+ "|" + (mt.getDateLastPlayed()==null ? "" : PL_DATE.format(mt.getDateLastPlayed()))
						+ "\n");
				
				writer.write(mt.getFilepath() + "\n");
				n ++;
			}
			logger.fine("Wrote " + n + " lines to '" + filePath + "'.");
			
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
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		MediaPlaylist other = (MediaPlaylist) obj;
		if (filePath == null) {
			if (other.filePath != null) {
				return false;
			}
		} else if (!filePath.equals(other.filePath)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		// Since equals() only uses filePath, we can do this.
		return filePath.hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

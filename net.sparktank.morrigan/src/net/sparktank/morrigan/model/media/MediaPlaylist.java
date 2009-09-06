package net.sparktank.morrigan.model.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class MediaPlaylist extends MediaList {
	
	private String filePath = null;
	
	public MediaPlaylist(String filePath) {
		super(getFilenameFromPath(filePath));
		this.filePath = filePath;
		ReloadFromFile();
	}
	
	public void ReloadFromFile () {
		File file = new File(filePath);
        BufferedReader reader = null;
        
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e); // FIXME
		}
		
		// repeat until all lines is read
		String text = null;
		try {
			while ((text = reader.readLine()) != null) {
				addTrack(text);
			}
		} catch (IOException e) {
			throw new RuntimeException(e); // FIXME
		}
	}
	
	public void WriteToFile () {
		// TODO write this.
	}
	
	private static String getFilenameFromPath (String filePath) {
		int x = filePath.lastIndexOf(File.separator);
		if (x>0) {
			return filePath.substring(x+1);
		} else {
			return filePath;
		}
	}
	
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
	
//	@Override
//	public int hashCode() {
//		return super.hashCode();
//	}


}

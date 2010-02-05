package net.sparktank.morrigan.helpers;

import java.util.List;
import java.util.Random;

import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaTrack;

public class OrderHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static enum PlaybackOrder {
		
		sequential {
			@Override
			public String toString() {
				return "sequential";
			}
		},
		
		random {
			@Override
			public String toString() {
				return "random";
			}
		},
		
//		bystartcount {
//			@Override
//			public String toString() {
//				return "by start-count";
//			}
//		},
		
//		bylastplayed {
//			@Override
//			public String toString() {
//				return "by last-played";
//			}
//		} 
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaTrack getNextTrack (MediaList list, MediaTrack track, PlaybackOrder mode) {
		switch (mode) {
			case sequential:
				return getNextTrackSequencial(list, track);
			
			case random:
				return getNextTrackRandom(list, track);
			
			default:
				throw new IllegalArgumentException();
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static MediaTrack getNextTrackSequencial (MediaList list, MediaTrack track) {
		List<MediaTrack> mediaTracks = list.getMediaTracks();
		if (mediaTracks.contains(track)) {
			int i = mediaTracks.indexOf(track) + 1;
			if (i >= mediaTracks.size()) i = 0;
			
			return mediaTracks.get(i);
			
		} else {
			return null;
		}
	}
	
	private static MediaTrack getNextTrackRandom (MediaList list, MediaTrack track) {
		Random generator = new Random();
		List<MediaTrack> mediaTracks = list.getMediaTracks();
		int i = generator.nextInt(mediaTracks.size());
		return mediaTracks.get(i);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
